# Opcode `a627` – Tabela de comandos

| Comando | Regra principal |
|---|---|
| `@exit` | Remove o próprio jogador da sala. |
| `@win` / `@lose` / `@timeout` / `@timeoutdm` | Somente admin (`character.position == 1`), encerra partida com status 1/0/2/3. |
| `@speed <n>` | Somente master, modos DEATHMATCH/BATTLE/TEAM_BATTLE, pré-start (`status == 0`), faixa 200..8000, salva em `STAT_SPEED`. |
| `@gauge <n>` | Mesmas regras de `@speed`, salva em `STAT_ATT_TRANS_GAUGE`. |
| `@reset` | Somente master e pré-start, limpa `statOverride`. |
| `@suicide` | Somente com partida iniciada (`status == 3`) e modo PLANET, chama `playerDeath()`. |
| `@kick <nome>` | Somente master, não permite auto-kick, remove alvo com reason `2`. |
| `@help` | Retorna lista de comandos. |
| `@stat-help` | Retorna ajuda de `@speed`, `@gauge`, `@reset`. |
| `@autosell` | Alterna `character.settings.autosell` e retorna estado ON/OFF. |
| desconhecido | Mensagem amigável de comando desconhecido. |

## Compatibilidade de parsing
- comando case-insensitive;
- separação por múltiplos espaços;
- payload string lido como null-terminated e charset Windows-1252.
