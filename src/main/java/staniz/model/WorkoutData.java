package staniz.model;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable workout values shared by storage, the service, and the JavaFX views.
 * Defensive copies keep an editor or a later plan change from modifying saved history.
 */
public final class WorkoutData {
    /**
     * Schema used for newly created state and accepted by the current model.
     */
    public static final int CURRENT_SCHEMA_VERSION = 2;

    /**
     * Maximum number of ordered workout days in a split, shared with the GUI.
     */
    public static final int MAX_WORKOUT_DAYS = 7;

    private WorkoutData() {
    }

    /**
     * One set, with an inclusive rep range and a nonnegative weight in kilograms.
     * Equal rep bounds represent an exact count.
     */
    public record WorkoutSet(double kg, int minReps, int maxReps) {
        /**
         * Validates weight and rep bounds at every construction, including JSON loading.
         */
        public WorkoutSet {
            require(Double.isFinite(kg) && kg >= 0, "Weight must be a finite number of kg, zero or greater.");
            require(minReps >= 1 && maxReps >= minReps,
                    "Reps must be positive, with the maximum at least the minimum.");
        }

        /**
         * Formats an exact count or the prescribed range without losing its meaning.
         */
        public String reps() {
            return minReps == maxReps ? Integer.toString(minReps) : minReps + "–" + maxReps;
        }
    }

    /**
     * An exercise occurrence in a workout; the same library exercise can appear on different days.
     */
    public record WorkoutExercise(String exerciseId, String name, List<WorkoutSet> sets) {
        public WorkoutExercise(String name, List<WorkoutSet> sets) {
            this("", name, sets);
        }
        /**
         * Requires a name and at least one set, then freezes the set list.
         */
        public WorkoutExercise {
            require(exerciseId != null, "Exercise identity is required.");
            name = text(name, "Exercise name");
            sets = List.copyOf(sets);
            require(!sets.isEmpty(), "An exercise needs at least one set.");
        }
    }

    /**
     * One ordered workout day. An empty exercise list is allowed while planning.
     * The ID keeps the current day stable when the split is reordered.
     */
    public record WorkoutDay(String id, String name, List<WorkoutExercise> exercises) {
        /**
         * Validates the day's identity and keeps an immutable exercise list.
         */
        public WorkoutDay {
            id = text(id, "Day ID");
            name = text(name, "Workout day name");
            exercises = List.copyOf(exercises);
        }
    }

    /**
     * Whether a history entry preserves the prescription or records exact performed reps.
     */
    public enum Recording {
        AS_PLANNED, WITH_CHANGES
    }

    /**
     * A self-contained historical snapshot that does not refer to the mutable active split.
     */
    public record WorkoutRecord(String id, String dayId, String completedAt, String dayName, Recording recording,
                                List<WorkoutExercise> exercises) {
        public WorkoutRecord(String completedAt, String dayName, Recording recording,
                             List<WorkoutExercise> exercises) {
            this(UUID.randomUUID().toString(), "", completedAt, dayName, recording, exercises);
        }
        /**
         * Checks the timestamp and requires exact counts for a performed-workout record.
         */
        public WorkoutRecord {
            id = text(id, "Record ID");
            require(dayId != null, "Day identity is required.");
            Instant.parse(completedAt);
            dayName = text(dayName, "Workout day name");
            require(recording != null, "A recording type is required.");
            exercises = List.copyOf(exercises);
            if (recording == Recording.WITH_CHANGES) {
                require(exercises.stream().flatMap(exercise -> exercise.sets().stream())
                        .allMatch(set -> set.minReps() == set.maxReps()), "Recorded reps must be exact counts.");
            } else {
                require(!exercises.isEmpty(), "An as-planned record needs exercises.");
            }
        }
    }

    /**
     * The complete persisted state. A single document makes history and cycle advancement one save.
     * Versioning lets future releases reject incompatible data instead of guessing its meaning.
     */
    public record State(int version, List<Exercise> library, List<WorkoutDay> days,
                        int currentDay, List<WorkoutRecord> history, Session session) {
        /**
         * Enforces the split limit, valid position, and library references before accepting state.
         */
        public State {
            require(version == CURRENT_SCHEMA_VERSION, "Unsupported workout file version.");
            library = List.copyOf(library);
            days = resolvePlannedExercises(days, library);
            history = resolveHistoricalExercises(history, library);

            validateSplitPosition(days, currentDay);
            Map<String, Exercise> exercisesById = validateAndIndexLibrary(library);
            validatePlannedDays(days, exercisesById);
            validateHistory(history, exercisesById);
            validateSession(session, days, currentDay, exercisesById);
        }

        /**
         * Supplies names for existing editors while stable IDs remain in the stored library.
         */
        public List<String> exercises() {
            return library.stream().map(Exercise::name).toList();
        }

        /**
         * Supplies a small library on first use without imposing a routine on the user.
         */
        public static State initial() {
            List<Exercise> library = List.of("Bench press", "Squat", "Deadlift", "Overhead press",
                    "Barbell row", "Pull-up", "Lat pulldown", "Leg press", "Biceps curl", "Triceps pushdown")
                    .stream().map(name -> new Exercise(UUID.randomUUID().toString(), name, false)).toList();
            return new State(CURRENT_SCHEMA_VERSION, library, List.of(), 0, List.of(), null);
        }

        /**
         * Resolves name-only planned entries and retains an immutable day list.
         */
        private static List<WorkoutDay> resolvePlannedExercises(List<WorkoutDay> days, List<Exercise> library) {
            return days.stream().map(day -> new WorkoutDay(day.id(), day.name(),
                    link(day.exercises(), library))).toList();
        }

        /**
         * Resolves name-only history entries without replacing their snapshot names or values.
         */
        private static List<WorkoutRecord> resolveHistoricalExercises(List<WorkoutRecord> history,
                                                                      List<Exercise> library) {
            return history.stream().map(record -> new WorkoutRecord(record.id(), record.dayId(),
                    record.completedAt(), record.dayName(), record.recording(), link(record.exercises(), library)))
                    .toList();
        }

        /**
         * Checks the day limit and current position, including the empty split used during setup.
         */
        private static void validateSplitPosition(List<WorkoutDay> days, int currentDay) {
            require(days.size() <= MAX_WORKOUT_DAYS,
                    "A split can contain at most " + MAX_WORKOUT_DAYS + " workout days.");
            require(currentDay >= 0 && (days.isEmpty() ? currentDay == 0 : currentDay < days.size()),
                    "The current workout position is invalid.");
        }

        /**
         * Rejects duplicate names and IDs while building the lookup used by reference validation.
         */
        private static Map<String, Exercise> validateAndIndexLibrary(List<Exercise> library) {
            HashSet<String> names = new HashSet<>();
            Map<String, Exercise> exercisesById = new HashMap<>();
            for (Exercise exercise : library) {
                require(names.add(exercise.name().toLowerCase(Locale.ROOT)), "Exercise names must be unique.");
                require(exercisesById.putIfAbsent(exercise.id(), exercise) == null, "Exercise IDs must be unique.");
            }
            return exercisesById;
        }

        /**
         * Requires unique days and planned exercise names that match their library identities.
         */
        private static void validatePlannedDays(List<WorkoutDay> days, Map<String, Exercise> exercisesById) {
            HashSet<String> dayIds = new HashSet<>();
            for (WorkoutDay day : days) {
                require(dayIds.add(day.id()), "Workout day IDs must be unique.");
                for (WorkoutExercise exercise : day.exercises()) {
                    Exercise entry = exercisesById.get(exercise.exerciseId());
                    require(entry != null, "A planned exercise is missing from the library.");
                    require(entry.name().equals(exercise.name()),
                            "A planned exercise name does not match its library ID.");
                }
            }
        }

        /**
         * Checks record identities and exercise references while allowing names saved before a rename.
         */
        private static void validateHistory(List<WorkoutRecord> history, Map<String, Exercise> exercisesById) {
            HashSet<String> recordIds = new HashSet<>();
            for (WorkoutRecord record : history) {
                require(recordIds.add(record.id()), "History record IDs must be unique.");
                require(record.exercises().stream().allMatch(item -> exercisesById.containsKey(item.exerciseId())),
                        "A history exercise is missing from the library.");
            }
        }

        /**
         * Checks an active session's day and exercise identities, preserving its original names.
         */
        private static void validateSession(Session session, List<WorkoutDay> days, int currentDay,
                                            Map<String, Exercise> exercisesById) {
            if (session != null) {
                require(!days.isEmpty() && days.get(currentDay).id().equals(session.dayId()),
                        "An active session must belong to the current workout day.");
                require(session.exercises().stream().allMatch(item -> exercisesById.containsKey(item.exerciseId())),
                        "An active exercise is missing from the library.");
            }
        }
    }

    /**
     * A library identity and name fixed at creation; only archive status changes through the service.
     * Archived exercises stay available in existing plans and history.
     */
    public record Exercise(String id, String name, boolean archived) {
        /**
         * Validates the identity and display name.
         */
        public Exercise {
            id = text(id, "Exercise ID");
            name = text(name, "Exercise name");
        }
    }

    /**
     * A resumable session uses a snapshot of the plan and saves even unfinished input as text.
     */
    public record Session(String dayId, String dayName, String startedAt, List<SessionExercise> exercises) {
        /**
         * Freezes the session and validates its start timestamp.
         */
        public Session {
            dayId = text(dayId, "Day ID");
            dayName = text(dayName, "Workout name");
            Instant.parse(startedAt);
            exercises = List.copyOf(exercises);
            require(!exercises.isEmpty(), "A session needs exercises.");
        }
    }

    /**
     * One exercise occurrence in an active session, including its individually completed sets.
     */
    public record SessionExercise(String exerciseId, String name, List<SessionSet> sets) {
        /**
         * Validates identity and retains an immutable ordered set list.
         */
        public SessionExercise {
            exerciseId = text(exerciseId, "Exercise ID");
            name = text(name, "Exercise name");
            sets = List.copyOf(sets);
            require(!sets.isEmpty(), "A session exercise needs sets.");
        }
    }

    /**
     * Incomplete text is persisted for recovery; only completed sets must contain valid actual values.
     */
    public record SessionSet(WorkoutSet planned, String weight, String reps, boolean completed) {
        /**
         * Checks completed values before allowing them into saved performance history.
         */
        public SessionSet {
            require(planned != null && weight != null && reps != null, "Session set fields are required.");
            if (completed) {
                parseActual(weight, reps);
            }
        }
    }

    /**
     * Parses exact performed values consistently for session completion and corrections.
     */
    public static WorkoutSet parseActual(String weight, String reps) {
        try {
            require(reps.strip().matches("[0-9]+"), "Enter an exact positive rep count before completing a set.");
            int count = Integer.parseInt(reps.strip());
            return new WorkoutSet(Double.parseDouble(weight.strip()), count, count);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid kg weight and exact positive reps.", exception);
        }
    }

    /**
     * Gives version-one exercise names deterministic identities during migration.
     */
    public static String legacyId(String name) {
        return UUID.nameUUIDFromBytes(("exercise:" + name.toLowerCase(Locale.ROOT))
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static List<WorkoutExercise> link(List<WorkoutExercise> exercises, List<Exercise> library) {
        return exercises.stream().map(item -> {
            if (!item.exerciseId().isEmpty()) {
                return item;
            }
            Exercise entry = library.stream().filter(exercise -> exercise.name().equals(item.name()))
                    .findFirst().orElseThrow(() ->
                            new IllegalArgumentException("Exercise is missing from the library."));
            return new WorkoutExercise(entry.id(), item.name(), item.sets());
        }).toList();
    }

    /**
     * Normalizes a required name for use in the library and split.
     */
    public static String text(String value, String field) {
        require(value != null && !value.isBlank(), field + " cannot be blank.");
        return value.strip();
    }

    /**
     * Reports invalid user data before any state is changed.
     */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
