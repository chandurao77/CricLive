# /project:new-feature

Implement a new feature end-to-end.

## Instructions
1. Clarify requirements before starting — ask if anything is ambiguous
2. Design the data model and API contract first
3. Implement backend: entity → repository → service → controller
4. Implement frontend: API integration → state → UI components
5. Write unit and integration tests for all layers
6. Update OpenAPI/Swagger docs for any new endpoints
7. Add feature flag if the feature needs gradual rollout

## Checklist
- [ ] Backend implementation with tests
- [ ] Frontend implementation with tests
- [ ] API documented
- [ ] Security reviewed (auth/authz correct?)
- [ ] Database migration included if schema changes