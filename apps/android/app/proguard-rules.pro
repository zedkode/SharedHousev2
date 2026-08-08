# SharedHouse application-specific R8 rules.
#
# Keep this file intentionally small. AndroidX, Compose, Ktor and Kotlin serialization publish
# consumer rules for their own reflective boundaries. Add narrowly scoped rules here only when a
# verified optimized-build failure requires them.
