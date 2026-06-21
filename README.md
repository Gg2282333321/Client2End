# Client2End

> Minecraft server stress-testing addon for Meteor Client

**Client2End** — набор модулей для нагрузочного тестирования Minecraft серверов (Vanilla, Spigot, Paper, Aternos). Предназначен исключительно для законного пентеста собственных серверов.

## Модули

| Модуль | Описание | Нагрузка |
|---|---|---|
| **Spammer** | Спам пакетами (9 методов) | Move, TabComplete, CreativeBomb, Payload и другие |
| **ChestCrash** | Open/close сундука | Открытие/закрытие контейнера |
| **TCP Flood** | TCP-флуд через raw сокеты | Handshake + Login Start |

### Методы Spammer

- **Sprint** — ClientCommandC2SPacket (START/STOP спринт)
- **Move** — PlayerMoveC2SPacket.Full (телепорт X±0.5)
- **Attack** — PlayerInteractEntityC2SPacket (атака ближнего игрока)
- **Payload** — CustomPayloadC2SPacket (бренд)
- **PayloadBomb** — CustomPayloadC2SPacket (250 символов, уникальный)
- **Race** — Move + Sprint + Move (race condition)
- **CreativeBomb** — CreativeInventoryActionC2SPacket + NBT 250КБ
- **TabComplete** — RequestCommandCompletionsC2SPacket (32КБ)
- **InteractItem** — PlayerInteractItemC2SPacket (use предмета)

## Целевые сервера

| Сервер | Эффективность |
|---|---|
| **Spigot** | OOM за 30-60 сек (10k/тик) |
| **Paper** | packet-limiter (500/7с) — удержание под лимитом |
| **Velocity** | Нет прямого доступа до сервера |

## Сборка

```bash
./gradlew build
```

Готовый .jar в `build/libs/`.

## Disclaimer

Данный софт предназначен только для тестирования собственных серверов в рамках законного пентеста. Автор не несёт ответственности за использование в незаконных целях.
