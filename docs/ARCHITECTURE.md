# Architecture

```mermaid
flowchart TD
    subgraph UI["ui (Compose)"]
        S[Screens] -- events --> VM[ViewModels]
        VM -- "StateFlow&lt;UiState&gt;" --> S
    end
    subgraph Domain["domain"]
        V[Validation rules]
        UC[Use cases<br/>only where logic spans repositories]
    end
    subgraph Data["data"]
        R[Repositories<br/>single source of truth]
        DB[(Room cache)]
        OB[Outbox worker<br/>WorkManager]
        FS[Firestore / Auth / FCM<br/>data sources]
    end
    VM --> UC --> R
    VM --> R
    R -- "Flow (reads)" --> DB
    FS -- "snapshot listeners" --> DB
    R -- "writes pending rows" --> DB
    OB -- "uploads pending" --> FS
    FS <--> FB[(Firebase)]
    CF[Cloud Functions] --> FB
```

**Unidirectional data flow:** the UI sends events to the ViewModel. The ViewModel calls
repositories and exposes one immutable `UiState` per screen. The UI only reads Room via
repository `Flow`s. Remote changes always arrive through Room, so the UI works the same
online and offline.

Status: the M1 skeleton (theme, navigation, DI, CI) is in place; the data layer arrives in M2–M4.
