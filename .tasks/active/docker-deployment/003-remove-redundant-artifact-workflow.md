# 003 — Remove Redundant Artifact Workflow from CI Pipeline

## What to build
Remove the artifact upload/download step from the CI pipeline. The Docker workflow rebuilds from source via `mvn package` in the Dockerfile, making the artifact step redundant.

## Acceptance Criteria
- [ ] `sub_artifact_workflow.yml` removed or the reference to it removed from main workflow
- [ ] Main workflow goes directly from test → docker (no artifact step in between)
- [ ] CI pipeline still builds and publishes Docker image correctly

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `.github/workflows/main_test_build_docker.yml` | Remove the artifact job and its dependency from the workflow chain |

### Files to DELETE
| File | Reason |
|------|--------|
| `.github/workflows/sub_artifact_workflow.yml` | No longer referenced; entire purpose was uploading build artifacts that docker workflow doesn't use |

### Files to READ (for context)
| File | What to check |
|------|---------------|
| `.github/workflows/sub_docker_workflow.yml` | Confirm it builds from source (Dockerfile with mvn package), not from downloaded artifact |
| `Dockerfile` | Confirm multi-stage build compiles from source |

### Implementation Details

In `main_test_build_docker.yml`, the current chain is likely: test → artifact → docker.
Change to: test → docker. Update the `needs:` field on the docker job to depend on test instead of artifact.

## Done Definition
CI pipeline runs test → docker with no artifact step. Docker image builds correctly from source.
