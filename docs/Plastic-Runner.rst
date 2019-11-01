.. footer::

  *Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.*
  *This program and the accompanying materials are made available under the*
  *terms of the Eclipse Public License v1.0 which accompanies this distribution,*
  *and is available at http://www.eclipse.org/legal/epl-v10.html*

==============
Plastic Runner
==============
*Sep 27, 2019*

Plastic Runner was created to help speed up the morpher writer's development cycle. Without this
utility, the controller would have to be restarted for any morpher logic changes to take effect. The
utility consists of the following:

* plastic_runner.sh (the script used to invoke the feature)
* example.properties (an example properties file that supplies arguments to the script above)
* plastic-XXXX-exec.jar (the self-contained, isolated version of Plastic)
* morphers (directory for your test morphers)
* schemas (directory for your test schemas)
* classifiers (directory for your test classifiers)

To use the utility, make sure all components in the list above are in a directory. Copy the
properties file to a name of your choice like test.properties and edit that new file. An example
contents to that file is::

  in-schema-name = lci-input
  in-schema-version = 1.0
  in-schema-type = json
  out-schema-name = lci-output
  out-schema-version = 1.0
  out-schema-type = json
  payload-file = lci-input-payload.json
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
