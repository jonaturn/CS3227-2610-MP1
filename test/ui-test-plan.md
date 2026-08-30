# Staniz UI Test Plan

Run these cases with the project `test-ui` skill. Expected output entries are
ordered fragments; the banner and decorative separators are intentionally omitted.

## Case 1: Create and list all task types

Aim: Confirm that to-dos, deadlines, and events use their respective type markers and scheduling details.

Input:

```text
todo borrow book
deadline return book /by 2019-12-02
event project meeting /from 2019-12-02 /to 2019-12-03
list
bye
```

Expected output:

```text
added: [T][ ] borrow book
added: [D][ ] return book (by: Dec 02 2019)
added: [E][ ] project meeting (from: Dec 02 2019 to: Dec 03 2019)
1.[T][ ] borrow book
2.[D][ ] return book (by: Dec 02 2019)
3.[E][ ] project meeting (from: Dec 02 2019 to: Dec 03 2019)
Bye. Hope to see you again soon!
```

## Case 2: Mark each task type

Aim: Confirm that inherited status behavior works for every concrete task type without losing type-specific information.

Input:

```text
todo borrow book
deadline return book /by 2019-12-02
event project meeting /from 2019-12-02 /to 2019-12-03
mark 1
mark 2
mark 3
list
bye
```

Expected output:

```text
Nice! I've marked this task as done:
  [T][X] borrow book
Nice! I've marked this task as done:
  [D][X] return book (by: Dec 02 2019)
Nice! I've marked this task as done:
  [E][X] project meeting (from: Dec 02 2019 to: Dec 03 2019)
1.[T][X] borrow book
2.[D][X] return book (by: Dec 02 2019)
3.[E][X] project meeting (from: Dec 02 2019 to: Dec 03 2019)
```

## Case 3: Unmark a scheduled task

Aim: Confirm that an inherited status can be reversed while an event retains its start and end times.

Input:

```text
event project meeting /from 2019-12-02 /to 2019-12-03
mark 1
unmark 1
list
bye
```

Expected output:

```text
  [E][X] project meeting (from: Dec 02 2019 to: Dec 03 2019)
OK, I've marked this task as not done yet:
  [E][ ] project meeting (from: Dec 02 2019 to: Dec 03 2019)
1.[E][ ] project meeting (from: Dec 02 2019 to: Dec 03 2019)
```

## Case 4: Recover from blank and unknown commands

Aim: Confirm that basic input errors are explained and do not prevent later valid commands from succeeding.

Input:

```text

todo
todo borrow book
blah
list
bye
```

Expected output:

```text
OOPS! Please enter a command.
OOPS! A todo needs a description. Try: todo borrow book
added: [T][ ] borrow book
OOPS! I don't recognize that command. Try todo, deadline, event, list, find, mark, unmark, delete, or bye.
1.[T][ ] borrow book
```

## Case 5: Validate scheduled task fields

Aim: Confirm that every required deadline and event field is validated and only valid scheduled tasks are stored.

Input:

```text
deadline return book
deadline  /by 2019-12-02
deadline return book /by
deadline return book /by 2019-12-02
event project meeting
event project meeting /from 2019-12-02
event  /from 2019-12-02 /to 2019-12-03
event project meeting /from /to 2019-12-03
event project meeting /from 2019-12-02 /to
event project meeting /from 2019-12-02 /to 2019-12-03
list
bye
```

Expected output:

```text
OOPS! A deadline needs '/by'. Try: deadline return book /by 2019-12-02
OOPS! A deadline needs a description before '/by'.
OOPS! A deadline needs a due time after '/by'.
added: [D][ ] return book (by: Dec 02 2019)
OOPS! An event needs '/from' and '/to'. Try: event meeting /from 2019-12-02 /to 2019-12-03
OOPS! An event needs an end time after '/to'. Try: event meeting /from 2019-12-02 /to 2019-12-03
OOPS! An event needs a description before '/from'.
OOPS! An event needs a start time after '/from'.
OOPS! An event needs an end time after '/to'.
added: [E][ ] project meeting (from: Dec 02 2019 to: Dec 03 2019)
1.[D][ ] return book (by: Dec 02 2019)
2.[E][ ] project meeting (from: Dec 02 2019 to: Dec 03 2019)
```

## Case 6: Validate status command task numbers

Aim: Confirm that invalid mark and unmark numbers are explained without changing task status or stopping later commands.

Input:

```text
todo borrow book
mark
mark two
mark 0
mark 2
unmark -1
mark 1
unmark
unmark 1
list
bye
```

Expected output:

```text
added: [T][ ] borrow book
OOPS! 'mark' needs a task number. Try: mark 1
OOPS! The task number must be a whole number.
OOPS! There is no task numbered 0. Your list currently has 1 task(s).
OOPS! There is no task numbered 2. Your list currently has 1 task(s).
OOPS! There is no task numbered -1. Your list currently has 1 task(s).
  [T][X] borrow book
OOPS! 'unmark' needs a task number. Try: unmark 1
  [T][ ] borrow book
1.[T][ ] borrow book
```

## Case 7: Delete tasks and renumber the list

Aim: Confirm that deleting middle, last, and only remaining tasks preserves order and reports the correct count.

Input:

```text
todo first task
deadline middle task /by 2019-12-02
event last task /from 2019-12-02 /to 2019-12-03
delete 2
list
delete 2
delete 1
list
bye
```

Expected output:

```text
Noted. I've removed this task:
  [D][ ] middle task (by: Dec 02 2019)
Now you have 2 tasks in the list.
1.[T][ ] first task
2.[E][ ] last task (from: Dec 02 2019 to: Dec 03 2019)
Noted. I've removed this task:
  [E][ ] last task (from: Dec 02 2019 to: Dec 03 2019)
Now you have 1 task in the list.
Noted. I've removed this task:
  [T][ ] first task
Now you have 0 tasks in the list.
Here are the tasks in your list:
```

## Case 8: Reject invalid delete task numbers

Aim: Confirm that invalid delete commands are explained and leave the task list unchanged.

Input:

```text
todo borrow book
delete
delete two
delete 0
delete 2
list
bye
```

Expected output:

```text
added: [T][ ] borrow book
OOPS! 'delete' needs a task number. Try: delete 1
OOPS! The task number must be a whole number.
OOPS! There is no task numbered 0. Your list currently has 1 task(s).
OOPS! There is no task numbered 2. Your list currently has 1 task(s).
1.[T][ ] borrow book
```

## Case 9: Load all task types after restart

Aim: Confirm that task types, order, completion status, and escaped text survive an application restart.

Input:

```text
todo read C:\docs | notes
deadline return book /by 2019-12-02
event project meeting /from 2019-12-02 /to 2019-12-03
mark 2
bye
<restart>
list
bye
```

Expected output:

```text
added: [T][ ] read C:\docs | notes
added: [D][ ] return book (by: Dec 02 2019)
added: [E][ ] project meeting (from: Dec 02 2019 to: Dec 03 2019)
  [D][X] return book (by: Dec 02 2019)
1.[T][ ] read C:\docs | notes
2.[D][X] return book (by: Dec 02 2019)
3.[E][ ] project meeting (from: Dec 02 2019 to: Dec 03 2019)
```

## Case 10: Persist unmark and delete operations

Aim: Confirm that status reversals and deletions remain applied across multiple application restarts.

Input:

```text
todo borrow book
deadline return book /by 2019-12-02
event project meeting /from 2019-12-02 /to 2019-12-03
mark 2
bye
<restart>
unmark 2
delete 1
bye
<restart>
list
bye
```

Expected output:

```text
  [D][X] return book (by: Dec 02 2019)
  [D][ ] return book (by: Dec 02 2019)
  [T][ ] borrow book
Now you have 2 tasks in the list.
1.[D][ ] return book (by: Dec 02 2019)
2.[E][ ] project meeting (from: Dec 02 2019 to: Dec 03 2019)
```

## Case 11: Validate calendar dates and event order

Aim: Confirm that valid dates are accepted while malformed, impossible, and reversed dates are rejected safely.

Input:

```text
deadline leap day /by 2024-02-29
deadline wrong format /by 29-02-2024
deadline impossible date /by 2023-02-29
event invalid start /from 03-01-2024 /to 2024-03-02
event invalid end /from 2024-03-01 /to 03-02-2024
event reversed /from 2024-03-02 /to 2024-03-01
event same day /from 2024-03-01 /to 2024-03-01
list
bye
```

Expected output:

```text
added: [D][ ] leap day (by: Feb 29 2024)
OOPS! The deadline date must use yyyy-MM-dd, e.g. 2019-12-02.
OOPS! The deadline date must use yyyy-MM-dd, e.g. 2019-12-02.
OOPS! The event start date must use yyyy-MM-dd, e.g. 2019-12-02.
OOPS! The event end date must use yyyy-MM-dd, e.g. 2019-12-02.
OOPS! The event start date cannot be after the end date.
added: [E][ ] same day (from: Mar 01 2024 to: Mar 01 2024)
1.[D][ ] leap day (by: Feb 29 2024)
2.[E][ ] same day (from: Mar 01 2024 to: Mar 01 2024)
```

## Case 12: Find tasks by description keyword

Aim: Confirm that find displays matches in order, rejects a missing keyword safely, and handles no matches.

Input:

```text
todo read book
deadline return book /by 2019-12-02
todo buy groceries
find book
find
find absent
list
bye
```

Expected output:

```text
added: [T][ ] read book
added: [D][ ] return book (by: Dec 02 2019)
added: [T][ ] buy groceries
Here are the matching tasks in your list:
1.[T][ ] read book
2.[D][ ] return book (by: Dec 02 2019)
OOPS! 'find' needs a keyword. Try: find book
Here are the matching tasks in your list:
Here are the tasks in your list:
1.[T][ ] read book
2.[D][ ] return book (by: Dec 02 2019)
3.[T][ ] buy groceries
Bye. Hope to see you again soon!
```
