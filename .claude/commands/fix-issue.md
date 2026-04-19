# /project:fix-issue

Fix a bug or issue. Requires an issue number or description.

## Instructions
1. Understand the issue fully before writing any code
2. Identify the root cause — don't just treat symptoms
3. Write a failing test that reproduces the bug first
4. Implement the minimal fix to make the test pass
5. Ensure no existing tests break
6. Update documentation if the fix changes behavior
7. Add a comment referencing the issue number if non-obvious

## Output
- Summary of root cause
- Files changed and why
- Test added to prevent regression
- Any follow-up issues discovered