# Changelog
## 1.3.0
- UpdateChecker registers itself as Listener for PlayerJoinEvent and automatically sends messages to server operators
- Replaced anonymous classes with lambdas and method references
- Revamped User Agent string
- General code cleanup
- Changed version format to semantic

## 1.2
Added shading to example-pom.xml

## 1.1
Sending console message synchronously to avoid mixed messages when more than one plugin requires updates. The check is still async, of course. 