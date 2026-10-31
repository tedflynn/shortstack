# Project-specific R8 rules for release builds.
#
# Room, kotlinx.serialization, Compose and the other libraries ship their own
# consumer rules, so nothing is needed here yet. Add rules only for code that
# is reached by reflection (for example, classes named in strings).
