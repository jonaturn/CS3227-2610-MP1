package staniz.task;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the ordered collection of tasks and provides operations that modify it.
 */
public class TaskList {
    private final List<Task> tasks;

    /**
     * Creates an empty task list.
     */
    public TaskList() {
        this(List.of());
    }

    /**
     * Creates a task list containing the supplied tasks in their current order.
     * A defensive copy prevents callers from modifying the collection directly.
     *
     * @param tasks initial tasks
     */
    public TaskList(List<Task> tasks) {
        this.tasks = new ArrayList<>(tasks);
    }

    /**
     * Adds a task to the end of the list.
     *
     * @param task task to add
     */
    public void add(Task task) {
        tasks.add(task);
    }

    /**
     * Returns the task at a zero-based index.
     *
     * @param index zero-based task index
     * @return task at the index
     */
    public Task get(int index) {
        return tasks.get(index);
    }

    /**
     * Marks the task at a zero-based index as completed.
     *
     * @param index zero-based task index
     * @return task whose status changed
     */
    public Task markAsDone(int index) {
        Task task = get(index);
        task.markAsDone();
        return task;
    }

    /**
     * Marks the task at a zero-based index as incomplete.
     *
     * @param index zero-based task index
     * @return task whose status changed
     */
    public Task markAsNotDone(int index) {
        Task task = get(index);
        task.markAsNotDone();
        return task;
    }

    /**
     * Removes and returns the task at a zero-based index.
     *
     * @param index zero-based task index
     * @return removed task
     */
    public Task delete(int index) {
        return tasks.remove(index);
    }

    /**
     * Finds tasks whose descriptions contain the given keyword.
     * Matching tasks retain their original order in a separate task list.
     *
     * @param keyword text to find in task descriptions.
     * @return task list containing all matching tasks.
     */
    public TaskList find(String keyword) {
        List<Task> matchingTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (task.hasDescriptionContaining(keyword)) {
                matchingTasks.add(task);
            }
        }
        return new TaskList(matchingTasks);
    }

    /**
     * Returns the number of stored tasks.
     *
     * @return task count
     */
    public int size() {
        return tasks.size();
    }

    /**
     * Returns an immutable snapshot for read-only consumers such as storage.
     *
     * @return tasks in their current order
     */
    public List<Task> getTasks() {
        return List.copyOf(tasks);
    }
}
