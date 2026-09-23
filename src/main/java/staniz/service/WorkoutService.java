package staniz.service;

import static staniz.model.WorkoutData.CURRENT_SCHEMA_VERSION;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import staniz.model.WorkoutData;
import staniz.model.WorkoutData.Exercise;
import staniz.model.WorkoutData.Recording;
import staniz.model.WorkoutData.Session;
import staniz.model.WorkoutData.SessionExercise;
import staniz.model.WorkoutData.SessionSet;
import staniz.model.WorkoutData.State;
import staniz.model.WorkoutData.WorkoutDay;
import staniz.model.WorkoutData.WorkoutExercise;
import staniz.model.WorkoutData.WorkoutRecord;
import staniz.model.WorkoutData.WorkoutSet;
import staniz.storage.StorageException;
import staniz.storage.WorkoutStore;

/**
 * Owns workout operations. Every change is validated and saved before becoming visible in memory.
 * Controllers can discard editing drafts without altering the live split.
 */
public class WorkoutService {
    private final WorkoutStore store;
    private final Clock clock;
    private State state;

    public WorkoutService(WorkoutStore store) throws StorageException {
        this(store, Clock.systemUTC());
    }

    /**
     * Loads saved state with a caller-provided clock for deterministic history tests.
     */
    public WorkoutService(WorkoutStore store, Clock clock) throws StorageException {
        this.store = store;
        this.clock = clock;
        state = store.load();
    }

    public State getState() {
        return state;
    }

    /**
     * Adds a trimmed, case-insensitively unique exercise name.
     */
    public void addExercise(String input) throws StorageException {
        String name = WorkoutData.text(input, "Exercise name");
        WorkoutData.require(state.exercises().stream().noneMatch(item -> item.equalsIgnoreCase(name)),
                "That exercise is already in your library.");
        List<Exercise> exercises = new ArrayList<>(state.library());
        exercises.add(new Exercise(UUID.randomUUID().toString(), name, false));
        commit(new State(CURRENT_SCHEMA_VERSION, exercises, state.days(),
                state.currentDay(), state.history(), state.session()));
    }

    /**
     * Creates an initially empty day at the end of the cycle.
     */
    public void addDay(String name) throws StorageException {
        List<WorkoutDay> days = new ArrayList<>(state.days());
        days.add(new WorkoutDay(UUID.randomUUID().toString(), name, List.of()));
        saveDays(days);
    }

    /**
     * Replaces one day's draft, preserving its identity and the current position.
     */
    public void updateDay(int index, String name, List<WorkoutExercise> exercises) throws StorageException {
        requireDay(index);
        exercises = resolveChoices(exercises, state.days().get(index).exercises());
        List<WorkoutDay> days = new ArrayList<>(state.days());
        days.set(index, new WorkoutDay(days.get(index).id(), name, exercises));
        saveDays(days);
    }

    /**
     * Removes a day; deleting the current day selects its successor, wrapping if necessary.
     */
    public void removeDay(int index) throws StorageException {
        requireDay(index);
        List<WorkoutDay> days = new ArrayList<>(state.days());
        days.remove(index);
        saveDays(days);
    }

    /**
     * Moves a day by one position while retaining the same current workout.
     */
    public void moveDay(int index, int direction) throws StorageException {
        requireDay(index);
        WorkoutData.require(direction == -1 || direction == 1, "Move a day up or down one position.");
        requireDay(index + direction);
        List<WorkoutDay> days = new ArrayList<>(state.days());
        WorkoutDay day = days.remove(index);
        days.add(index + direction, day);
        saveDays(days);
    }

    /**
     * Advances without creating a completion record, including past an unfinished planning day.
     */
    public void skip() throws StorageException {
        requireDay(state.currentDay());
        requireNoSession();
        advance(state.history());
    }

    /**
     * Completes a nonempty workout without retaining performance data.
     */
    public void completeWithoutRecording() throws StorageException {
        requireWorkout();
        advance(state.history());
    }

    /**
     * Records the prescription, preserving rep ranges rather than inventing exact performed reps.
     */
    public void completeAsPlanned() throws StorageException {
        WorkoutDay day = requireWorkout();
        record(day.id(), day.name(), Recording.AS_PLANNED, day.exercises());
    }

    /**
     * Records exact performed sets. Omitted exercises and sets represent skipped work.
     * An empty list is allowed if the entire session's planned work was omitted.
     */
    public void completeWithChanges(List<WorkoutExercise> actual) throws StorageException {
        WorkoutDay day = requireWorkout();
        record(day.id(), day.name(), Recording.WITH_CHANGES, resolveChoices(actual, day.exercises()));
    }

    /**
     * Records either a quick completion or a finished session and advances with one save.
     * Callers validate their completion mode and supply the appropriate day or session snapshot.
     */
    private void record(String dayId, String dayName, Recording recording, List<WorkoutExercise> exercises)
            throws StorageException {
        List<WorkoutRecord> history = new ArrayList<>(state.history());
        history.add(0, new WorkoutRecord(UUID.randomUUID().toString(), dayId, Instant.now(clock).toString(),
                dayName, recording, exercises));
        advance(history);
    }

    private WorkoutDay requireWorkout() {
        requireNoSession();
        requireDay(state.currentDay());
        WorkoutDay day = state.days().get(state.currentDay());
        WorkoutData.require(!day.exercises().isEmpty(), "Add exercises to this workout before completing it.");
        return day;
    }

    /**
     * Saves history and the next cycle position together, clearing any finished active session.
     * Callers must validate that a workout exists and that their action is allowed during the current session state.
     */
    private void advance(List<WorkoutRecord> history) throws StorageException {
        commit(new State(CURRENT_SCHEMA_VERSION, state.library(), state.days(),
                (state.currentDay() + 1) % state.days().size(), history, null));
    }

    private void requireDay(int index) {
        WorkoutData.require(index >= 0 && index < state.days().size(), "Select a workout day first.");
    }

    /**
     * Finds the current day by ID after edits, or its successor when it has been removed.
     */
    private void saveDays(List<WorkoutDay> days) throws StorageException {
        requireNoSession();
        int current = days.isEmpty() ? 0 : state.currentDay() % days.size();
        if (!state.days().isEmpty()) {
            String currentId = state.days().get(state.currentDay()).id();
            for (int index = 0; index < days.size(); index++) {
                if (days.get(index).id().equals(currentId)) {
                    current = index;
                    break;
                }
            }
        }
        commit(new State(CURRENT_SCHEMA_VERSION, state.library(), days, current, state.history(), null));
    }

    /**
     * Hides or restores an exercise for new selections without deleting existing references.
     */
    public void archiveExercise(String id, boolean archived) throws StorageException {
        exercise(id);
        List<Exercise> library = state.library().stream().map(item -> item.id().equals(id)
                ? new Exercise(id, item.name(), archived) : item).toList();
        commit(new State(CURRENT_SCHEMA_VERSION, library, state.days(),
                state.currentDay(), state.history(), state.session()));
    }

    /**
     * Supplies active library choices plus archived exercises already present in the edited snapshot.
     */
    public List<String> choices(List<WorkoutExercise> existing) {
        List<String> choices = new ArrayList<>(state.library().stream().filter(entry -> !entry.archived())
                .map(Exercise::name).toList());
        existing.forEach(item -> {
            if (!choices.contains(item.name())) {
                choices.add(item.name());
            }
        });
        return List.copyOf(choices);
    }

    /**
     * Copies an entire workout into a new day, subject to the seven-day limit.
     */
    public void duplicateDay(int index, String name) throws StorageException {
        requireDay(index);
        List<WorkoutDay> days = new ArrayList<>(state.days());
        days.add(new WorkoutDay(UUID.randomUUID().toString(), name, days.get(index).exercises()));
        saveDays(days);
    }

    /**
     * Copies the source prescription to a target exercise, retaining the target exercise identity.
     */
    public void copySets(int sourceDay, int sourceExercise, int targetDay, int targetExercise) throws StorageException {
        requireDay(sourceDay);
        requireDay(targetDay);
        var source = state.days().get(sourceDay).exercises();
        var target = new ArrayList<>(state.days().get(targetDay).exercises());
        WorkoutData.require(sourceExercise >= 0 && sourceExercise < source.size()
                && targetExercise >= 0 && targetExercise < target.size(), "Select source and target exercises.");
        WorkoutExercise item = target.get(targetExercise);
        target.set(targetExercise,
                new WorkoutExercise(item.exerciseId(), item.name(), source.get(sourceExercise).sets()));
        updateDay(targetDay, state.days().get(targetDay).name(), target);
    }

    /**
     * Starts a recoverable snapshot of the current workout without advancing the split.
     */
    public void startSession() throws StorageException {
        WorkoutDay day = requireWorkout();
        List<SessionExercise> exercises = day.exercises().stream().map(item -> new SessionExercise(item.exerciseId(),
                item.name(), item.sets().stream().map(set -> new SessionSet(set, Double.toString(set.kg()),
                        set.minReps() == set.maxReps() ? Integer.toString(set.minReps()) : "", false))
                        .toList())).toList();
        saveSession(new Session(day.id(), day.name(), Instant.now(clock).toString(), exercises));
    }

    /**
     * Saves a single set draft or completion immediately, leaving the prescription unchanged.
     */
    public void updateSessionSet(int exerciseIndex, int setIndex, String weight, String reps, boolean done)
            throws StorageException {
        Session session = requireSession();
        WorkoutData.require(exerciseIndex >= 0 && exerciseIndex < session.exercises().size(), "Select an exercise.");
        List<SessionExercise> exercises = new ArrayList<>(session.exercises());
        SessionExercise exercise = exercises.get(exerciseIndex);
        WorkoutData.require(setIndex >= 0 && setIndex < exercise.sets().size(), "Select a set.");
        List<SessionSet> sets = new ArrayList<>(exercise.sets());
        sets.set(setIndex, new SessionSet(sets.get(setIndex).planned(), weight, reps, done));
        exercises.set(exerciseIndex, new SessionExercise(exercise.exerciseId(), exercise.name(), sets));
        saveSession(new Session(session.dayId(), session.dayName(), session.startedAt(), exercises));
    }

    /**
     * Discards the in-progress session without recording or advancing.
     */
    public void discardSession() throws StorageException {
        requireSession();
        saveSession(null);
    }

    /**
     * Records completed sets only, clears the active draft, and advances in one atomic state change.
     */
    public void finishSession() throws StorageException {
        Session session = requireSession();
        List<WorkoutExercise> actual = new ArrayList<>();
        for (SessionExercise exercise : session.exercises()) {
            List<WorkoutSet> sets = exercise.sets().stream().filter(SessionSet::completed)
                    .map(set -> WorkoutData.parseActual(set.weight(), set.reps())).toList();
            if (!sets.isEmpty()) {
                actual.add(new WorkoutExercise(exercise.exerciseId(), exercise.name(), sets));
            }
        }
        WorkoutData.require(!actual.isEmpty(), "Complete at least one set, or discard the session.");
        record(session.dayId(), session.dayName(), Recording.WITH_CHANGES, actual);
    }

    /**
     * Corrects a saved snapshot in place while preserving its identity, date, and recording type.
     */
    public void correctRecord(String id, String dayName, List<WorkoutExercise> exercises) throws StorageException {
        List<WorkoutRecord> history = new ArrayList<>(state.history());
        int index = -1;
        for (int position = 0; position < history.size(); position++) {
            if (history.get(position).id().equals(id)) {
                index = position;
                break;
            }
        }
        WorkoutData.require(index >= 0, "Select a history record.");
        WorkoutRecord previous = history.get(index);
        exercises = resolveChoices(exercises, previous.exercises());
        history.set(index, new WorkoutRecord(id, previous.dayId(), previous.completedAt(),
                dayName, previous.recording(), exercises));
        commit(new State(CURRENT_SCHEMA_VERSION, state.library(), state.days(),
                state.currentDay(), history, state.session()));
    }

    /**
     * Finds the newest exact recording by exercise identity, including snapshots saved by older app versions.
     */
    public Optional<WorkoutRecord> previousPerformance(String exerciseId) {
        return performance(exerciseId).stream()
                .max(Comparator.comparing(record -> Instant.parse(record.completedAt())));
    }

    /**
     * Returns exact performance records in chronological order for charts and comparison.
     */
    public List<WorkoutRecord> performance(String exerciseId) {
        return state.history().stream().filter(record -> record.recording() == Recording.WITH_CHANGES
                && record.exercises().stream().anyMatch(item -> item.exerciseId().equals(exerciseId)))
                .sorted(Comparator.comparing(record -> Instant.parse(record.completedAt()))).toList();
    }

    /**
     * Searches snapshots by workout name and exercise identity, including the exercise's current name.
     */
    public List<WorkoutRecord> filterHistory(String workout, String exerciseId) {
        String query = workout.strip().toLowerCase(Locale.ROOT);
        return state.history().stream().filter(record -> record.dayName().toLowerCase(Locale.ROOT).contains(query))
                .filter(record -> exerciseId.isEmpty() || record.exercises().stream()
                        .anyMatch(item -> item.exerciseId().equals(exerciseId))).toList();
    }

    private Exercise exercise(String id) {
        return state.library().stream().filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select a library exercise."));
    }

    /**
     * Resolves editor selections and validates both identity and name before accepting recorded or planned data.
     * Only an existing snapshot's exact pair may retain a name saved before library renaming was removed.
     * Archived exercises can be retained only when already present in the edited workout.
     */
    private List<WorkoutExercise> resolveChoices(List<WorkoutExercise> exercises, List<WorkoutExercise> existing) {
        List<WorkoutExercise> resolved = new ArrayList<>();
        for (WorkoutExercise item : exercises) {
            Exercise entry = item.exerciseId().isEmpty() ? resolveChoiceName(item.name(), existing)
                    : exercise(item.exerciseId());
            boolean retained = existing.stream().anyMatch(previous -> previous.exerciseId().equals(entry.id())
                    && previous.name().equals(item.name()));
            WorkoutData.require(entry.name().equals(item.name()) || retained,
                    "The exercise name does not match its library ID.");
            WorkoutData.require(!entry.archived() || existing.stream()
                    .anyMatch(previous -> previous.exerciseId().equals(entry.id())),
                    "Choose an active exercise or restore it first.");
            resolved.add(new WorkoutExercise(entry.id(), item.name(), item.sets()));
        }
        return List.copyOf(resolved);
    }

    /**
     * New name-only selections prefer the library; older snapshot names can resolve through their saved identity.
     */
    private Exercise resolveChoiceName(String name, List<WorkoutExercise> existing) {
        return state.library().stream().filter(entry -> entry.name().equals(name)).findFirst()
                .orElseGet(() -> existing.stream().filter(item -> item.name().equals(name))
                        .map(item -> exercise(item.exerciseId())).findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException("Choose an active exercise or restore it first.")));
    }

    private void requireNoSession() {
        WorkoutData.require(state.session() == null, "Finish or discard your active session first.");
    }

    private Session requireSession() {
        WorkoutData.require(state.session() != null, "Start a workout session first.");
        return state.session();
    }

    private void saveSession(Session session) throws StorageException {
        commit(new State(CURRENT_SCHEMA_VERSION, state.library(), state.days(),
                state.currentDay(), state.history(), session));
    }

    private void commit(State next) throws StorageException {
        store.save(next);
        state = next;
    }
}
