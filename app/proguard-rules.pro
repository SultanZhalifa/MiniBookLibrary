# Mini Book Library - R8 rules
#
# Nothing in the app uses reflection: JSON is mapped by hand, Room/Navigation/ML Kit/Coil
# ship their own consumer rules, and classes named in XML (fragments in nav graphs, the
# widget receiver) are kept automatically by AAPT. So the only rules here are for
# readable crash stack traces.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
