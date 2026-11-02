package com.cardinalstar.cubicchunks.api.registry;

/// A dependency graph is something that stores a list of named objects. Each object has a list of dependencies, and
/// these dependencies determine the order that the contained objects will be processed in.
/// This is typically used to control the ordering of third party integrations. If one integration needs to run after or
/// before another, it will add a `before:xyz` or `after:xyz` dependency on the other integration.
/// Objects and dependencies can be added or removed at any point, but this is discouraged because it makes debugging
/// very difficult.
/// Circular dependencies between objects will cause a runtime error and must be avoided.
public interface IDependencyGraph<T> {

    /// The current object must run after another one. These dependencies can be made optional by adding a question mark
    /// suffix: `requires:foo?`. An optional dependency will not throw an error if the dependent object is missing.
    String REQUIRES = "requires:";
    /// The current object will always run after another one. This is always optional.
    String AFTER = "after:";
    /// The opposite of [#REQUIRES]. Ths current object will run before another one. As with [#REQUIRES], this can be
    /// made optional by adding a question mark suffix: `required-by:bar?`.
    String REQUIRED_BY = "required-by:";
    /// The opposite of [#AFTER]. The current object will always run before another one. As with [#AFTER], this is
    /// always optional.
    String BEFORE = "before:";

    /// Adds a named object with one or more dependency specifications (see above)
    void addObject(String name, T value, String... deps);
}
