.. footer::

  *Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.*
  *This program and the accompanying materials are made available under the*
  *terms of the Eclipse Public License v1.0 which accompanies this distribution,*
  *and is available at http://www.eclipse.org/legal/epl-v10.html*

======================
Plastic Best Practices
======================
*Sep 27, 2019*

- Always use a unique name for each morpher. They are all in the same namespace and
  will collide if they have the same name. Ideally the morpher class name will include
  trailing numbers to account for versioning. There is no error check for this yet.

- In general, input and output variable names should be the same to use the built-in
  mapping, to avoid writing a morpher, and to make it easier to follow.

- If you write a morpher and deal with conditional output, it is better to populate the
  full output in the output schema and delete portions in the morpher.

- Never use periods as part of your variable names because Plastic uses these internally
  as path separators. There is no error check for this condition yet.

- Treat the parsed payload as read-only; don't modify it in your morpher. Input maps, output
  maps, and the output tree are fully modifiable.

- Classes in the lib area are for importing and should have their names match their
  file name (just like Java). You can use an aribtrary directory heirarchy if you avoid
  using packages for your classes (recommended). If you insist on using packages, then the
  directory structure must match the package names.

- Classifiers and morphers should never die - they should gracefully degrade. This means
  they should not abort, throw, or return null. Doing otherwise will make the individual
  translation fail, or if being used in a batching context, the whole batch will fail
  translation.


Appendix
~~~~~~~~
This document can be converted to PDF using `rst2pdf
<https://github.com/rst2pdf/rst2pdf>`_

`RST syntax reference
<http://docutils.sourceforge.net/docs/user/rst/quickref.html>`_
