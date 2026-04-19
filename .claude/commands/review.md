# /project:review

Perform a thorough code review of the current changes or specified files.

## Instructions
1. Check code style against `rules/code-style.md`
2. Verify tests exist and follow `rules/testing.md`
3. Review API changes against `rules/api-conventions.md`
4. Run security checks per `rules/security.md`
5. Look for performance issues, N+1 queries, memory leaks
6. Check for proper error handling and logging
7. Verify no secrets or sensitive data are exposed

## Output Format
Provide feedback as:
- ✅ **Approved** — with any minor suggestions
- 🔶 **Needs Changes** — list required fixes
- ❌ **Blocked** — list critical issues that must be resolved

Be specific: include file names, line references, and suggested fixes.