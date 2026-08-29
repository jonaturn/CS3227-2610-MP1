# Staniz UI Test Plan

Run these cases with the project `test-ui` skill. Expected output entries are ordered fragments; the banner and decorative separators are intentionally omitted.

## Case 1: Create and list all task types

Aim: Confirm that to-dos, deadlines, and events are created and displayed using their respective type markers and scheduling details.

Input:

```text
todo borrow book
deadline return book /by Sunday
event project meeting /from Mon 2pm /to 4pm
list
bye
```

Expected output:

```text
added: [T][ ] borrow book
added: [D][ ] return book (by: Sunday)
added: [E][ ] project meeting (from: Mon 2pm to: 4pm)
1.[T][ ] borrow book
2.[D][ ] return book (by: Sunday)
3.[E][ ] project meeting (from: Mon 2pm to: 4pm)
Bye. Hope to see you again soon!
```

## Case 2: Mark each task type

Aim: Confirm that inherited status behavior works for every concrete task type without losing type-specific information.

Input:

```text
todo borrow book
deadline return book /by Sunday
event project meeting /from Mon 2pm /to 4pm
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
  [D][X] return book (by: Sunday)
Nice! I've marked this task as done:
  [E][X] project meeting (from: Mon 2pm to: 4pm)
1.[T][X] borrow book
2.[D][X] return book (by: Sunday)
3.[E][X] project meeting (from: Mon 2pm to: 4pm)
```

## Case 3: Unmark a scheduled task

Aim: Confirm that an inherited status can be reversed while an event retains its start and end times.

Input:

```text
event project meeting /from Mon 2pm /to 4pm
mark 1
unmark 1
list
bye
```

Expected output:

```text
  [E][X] project meeting (from: Mon 2pm to: 4pm)
OK, I've marked this task as not done yet:
  [E][ ] project meeting (from: Mon 2pm to: 4pm)
1.[E][ ] project meeting (from: Mon 2pm to: 4pm)
```
