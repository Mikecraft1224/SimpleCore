# ToDo / Requirements

## Config
### Short description
Annotation-based configuration / GUI for categories and fields. Categories are collapsible (default: collapsed or open) and support subclasses.

### Data types
- Primitives: `int`, `string`, `bool`
- Lists:
    - Immutable and mutable
    - Element types: user-definable
    - Support custom default values

### Field / UI components
- Dropdown
- Color picker
- Buttons
- Slider with numeric text input (start, step, end)
- Separator (above)
- Text field
- Search field across categories

### Visibility / configuration flags
- `excludeFromVisuals` — field not shown in GUI
- `excludeFromConfig` — field not saved in configuration file

### Behavior / persistence
- Persist collapsed state per category (default: collapsed / open)
- Annotations define mapping between data and UI
- Search filters categories and fields

### Implementation notes
- Languages: Kotlin / Java, Build: Gradle
- Implement via Annotation Processor or Reflection for automatic UI generation
- Generate/use subclasses for special collapsible categories
- Tests for list types, default values, and persistence of collapsed state

### Example usage
```kotlin
TODO
```


## Eventbus
### Short description
Lightweight eventbus system for decoupled communication between components.

### Features
- Subscribe to events with specific types
- Easy to create own events and post to the same global bus that is shared between all subscribed mods
- Opt-in to event bus system in each mod
- Barebones implementation to allow easy creation of own eventbus systems if needed

### Example usage

```kotlin