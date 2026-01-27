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


## Ui Library
### Short description
Easy to expand moveables and some predefined UIs

### Components
- Some sort of base overlay class that provides position and is moveable
- Default UIs that have text content with optional images in front of each row, categories and clickable rows
- Color customization