.. footer::

  *Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.*
  *This program and the accompanying materials are made available under the*
  *terms of the Eclipse Public License v1.0 which accompanies this distribution,*
  *and is available at http://www.eclipse.org/legal/epl-v10.html*

==============
Plastic Runner
==============
*Jan 17, 2020*

Plastic Runner was created to help speed up the morpher writer's development cycle. It is a full deployment
of Plastic in a single directory. It is intended to allow quick changes along with testing of those changes.
It typically takes just a small number of seconds to try out your changes to schemas, morphers, and
classifiers.

The utility consists of the following:

* plastic_runner.sh (the script used to invoke the feature)
* example.properties (an example properties file that supplies arguments to the script above)
* plastic-XXXX-exec.jar (the self-contained, isolated version of Plastic)
* morphers (directory for your test morphers)
* schemas (directory for your test schemas)
* classifiers (directory for your test classifiers)

Plastic Runner comes in a .tar.gz and can be obtained from any Plastic build. You may also run it directly
from the target/plasticrunner directory after you do a build.

.. warning::

  Note that you can opt use the target/plasticrunner but the entire target directory is
  wiped out on each build, including any of your files. So be aware of this risk.

To use the utility, make sure all components in the list above are in a directory. Copy the
properties file to a name of your choice like test.properties and edit that new file. An example
contents to that file is::

  in-schema-name = lmn-input
  in-schema-version = 1.0
  in-schema-type = json
  out-schema-name = lmn-output
  out-schema-version = 1.0
  out-schema-type = json
  payload-file = lmn-input-payload.json
  defaults-file = my-defaults.json

The parameters are fairly self-explanatory for experienced morpher writers. Note that the
input schema and output schema must exist in the morphers directory or you will get an
error. The payload file must exist and must match the input schema of course. The defaults
file is optional - just leave it's value blank if you are not using it. Its format is a
simple JSON object, where the keys are the variable names and the values are the variable
values. An example is::

  {
    "adminStatus": "UP",
    "mtu": "1500"
  }

The equivalent example, but for an XML-based defaults file would be::

   <Map>
      <Entry>
          <Key>adminStatus</Key>
          <Value>UP</Value>
      </Entry>
      <Entry>
          <Key>mtu</Key>
          <Value>1500</Value>
      </Entry>
   </Map>

To obtain the latest utility, please contact the Lumina development team. Remember that the
utility has an independent version of Plastic which may not match the version deployed
in the controller.


Appendix
~~~~~~~~
This document can be converted to PDF using `rst2pdf
<https://github.com/rst2pdf/rst2pdf>`_

`RST syntax reference
<http://docutils.sourceforge.net/docs/user/rst/quickref.html>`_
