# finding

## Agent skills

### Issue tracker

GitHub Issues (`git@github.com:daodaoq/finding.git`); PRs are **not** a triage surface. See `docs/agents/issue-tracker.md`.

### Triage labels

Default label vocabulary — all five canonical roles use their standard names (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context — one `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## Git 工作流约定

- **默认只提交到本地分支,不推送远程。** 推送 `origin/master` 会触发生产服务器自动部署(GitHub WebHook → `deploy.sh`,云端重新构建并重启服务),属于对外动作。
- 只有用户**明确说「推送 GitHub / 推送到 github」**时才执行 `git push origin master`。
- 未获明确指令时:commit 后停在本地,并在回复里提示"已提交本地,需要推送吗"。
- 涉及生产数据的操作(执行 SQL 迁移、改服务器配置等)同样先确认再执行。
