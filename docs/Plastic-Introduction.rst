.. footer::

  *Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.*
  *This program and the accompanying materials are made available under the*
  *terms of the Eclipse Public License v1.0 which accompanies this distribution,*
  *and is available at http://www.eclipse.org/legal/epl-v10.html*

====================
Plastic Introduction
====================
*Sep 27, 2019*

Introduction
~~~~~~~~~~~~
This document forms the introduction to Plastic, but there are several
other supporting documents that give more details, depending upon your needs.

**Totorial - No Coding** - this covers concepts and usage for invoking transformations
using declarative schemas only without usage of coded plug-ins to change the
default translation behavior.

**Runner** - this covers the execution of the runner tool that allows you to
quickly develop schemas and any of the optional plug-ins.

**Tutorial - Coding** - this covers details of how and why to use optional plug-ins
called classifiers and morphers.

**Authoring** - this is a pseudo-reference for the support library for writers
of classifiers and morphers.

**Best Practices** - this is an enumeration of conventional best practices that
will make use of this translation facility easier.

(Former internal name of this project was Cartographer; you might see some deprecated references)

Pipeline
~~~~~~~~
Plastic has a pipeline of steps that it uses for each transformations. This
series of steps is always followed, but some steps are optional, depending on
the optional presence of plugins called classifiers and morphers. You need to
understand this sequence only if you are writing classifiers or morphers.

Simplified Pipeline
~~~~~~~~~~~~~~~~~~~

#. Parse the payload
#. If present, invoke the classifier
#. Load the input schema
#. Bind the variables
#. Load the output schema and create a rough draft of the output
#. If present, invoke any morphers
#. Serialize and emit the output

Detailed Pipeline
~~~~~~~~~~~~~~~~~

You can just skim over the description of this pipeline and look back and
reference it as needed later.

#. Create input tree by parsing the payload
#. **Callback classifier** to disambiguate input schema name
#. Load optional input morpher and output morpher
#. Parse input schema and locate $ input variables
#. Bind values to $ input variables by walking payload
#. If any default values as part of translate call, bind their values
#. **Callback tweakInputs()** for input morpher then output morpher
#. Parse output schema and locate $ output variables
#. Bind values to $ output variables by using values from $ input variables
#. **Callback tweakValues()** for input morpher then output morpher
#. Instantiate the output tree using the output schema as a template
#. Inject $ output values into the output tree
#. **Callback tweakParsed()** for input morpher then output morpher
#. Validate by looking for dangling/unused $ input and $ output variables
#. Serialize and emit the output tree

Components
~~~~~~~~~~
Plastic requires several components to support the pipeline. They all can be
found at the "Plastic root", a directory that forms the parent of the child
directories. Morphers are found in the "morphers" child directory. Schemas are
found in the "schemas" directory. Classifiers are found in the "classifiers"
directory. Ad hoc support logic is found in the "lib" child directory.

Variables
~~~~~~~~~
Both input and output schemas can have variables declared in them using the dollar
variable syntax, like "${abc}". You can see more examples in the schema snippets
later in this document. You can now statically bind a default value to a variable
so that if that value is not present in the payload, it will still have a binding.
An example is::

  ${abc=123}

where **abc** is the variable name and **123** is the default value. The equals
sign separates the value from the variable name. The following example shows
**abc** with a default value of a blank string::

  ${abc=}

Note that dynamic defaults, which are supplied in the actual translate call,
will take priority over static defaults.

Schemas
~~~~~~~
Plastic has a schemas directory that holds input and output schemas used in mapping.
A "translate" call requires an input schema, an output schema, and a payload. The schemas
are either XML or JSON. An incoming payload must match the incoming schema; it is not
possible to have an XML payload and a JSON input schema.

An example input or output schema looks like this::

  {
    "adminStatus": "UP",
    "description": "${description}",
    "deviceName": "${deviceName}",
    "interfaceName": "${interfaceName}",
    "ipAddressesV4": [
      {
        "ip-address": "${ip4-address[*]}",
        "prefix-length": "${prefix4-length[*]}"
      }
    ],
    "ipAddressesV6": [
      {
        "ip-address": "${ip6-address[*]}",
        "prefix-length": "${prefix6-length[*]}"
      }
    ],
    "mtu": "${mtu}",
    "mplsEnable": false,
    "bundleId": "na",
    "vlanId": "${vlanId}",
    "subIntfId": "${subIntfId}",
    "operStatus": "UP",
    "deviceVendor": "juniper"
  }

Morphers
~~~~~~~~
Plastic has a morphers directory that holds optional groovy classes, called morphers,
that are used to do more sophisticated kinds of mapping. They must be named to match either
the full input schema name, full output schema name, or both, except that they end in .groovy
rather than .xml or .json. You can find example morphers in the morpher directory in the
installation area.

A simple morpher, showing the most commonly used callback hook, looks like this::

  import org.opendaylight.plastic.implementation.BasicMorpher

  class MySpecialMorpher extends BasicMorpher
  {
      void tweakValues(Map ins, Map outs) {
          outs['my-var-a'] = ins['my-var-b'] + " 123"
      }
  }


A full type signature for a morpher, showing all of the callback hooks,
looks like this::

  import org.opendaylight.plastic.implementation.BasicMorpher

  class MySpecialMorpher extends BasicMorpher
  {
      void tweakInputs(Map ins, Object payload) {
          ...
      }

      void tweakValues(Map ins, Map outs) {
          ...
      }

      void tweakParsed(Object inTree, Object outTree) {
          ...
      }
  }


The morpher must have at least one of those methods to be well-formed and
accepted by Plastic. Morphers and support logic are described in a
companion document Plastic-Authoring

Morpher Libraries
~~~~~~~~~~~~~~~~~
Morphers can use shared code by putting the shared code in the "lib" folder. The code should
be a file with a package statement at the top and a class definition. The package name can
be arbitrary per Groovy/Java name rules. The file can be in any arbitrary directory
structure since Plastic will recurse to load the files.

You must use a package statement. Leaving the package statement off puts the class in the
default package and there is no way to import that class into a non-default package.

Classifiers
~~~~~~~~~~~
Plastic has a classifiers directory that holds optional groovy classes,
called classifiers, that are used to examine incoming payloads to help
determine the input schema name. They also are used to control batching
translations. See Plastic-Authoring for more details.

Normally a translate call has an input schema name, version, and type like "my-schema",
"1.0", and "json". These are put together to form a file name like "my-schema-1.0.json"
and this file can live anywhere under the schemas directory.

When an schema name cannot be determined without examining the contents of
the incoming payload, then a classifier is used. To invoke a classifier,
make the translate call using an input like "my-schema-${EVENT_ID}", "1.0",
"json". Plastic will see the "${EVENT_ID}" and will look everywhere
inside the classifiers directory for a file named EVENT_ID.groovy.

A very simple classifier looks like this::

  import org.opendaylight.plastic.implementation.SimpleClassifier

  class GoodClassifier extends SimpleClassifier {
      String classify(Object parsedPayload) {
          "my-schema-a"
      }
  }

Note that the classify() method is called with a payload to examine. The
payload is either a map (representing parsed JSON) or a node (representing
parsed XML).

There is a more complex classifier available called a PlanningClassifier,
which uses a "translation plan". Such classifiers need to figure out the
base name for the schema and will call plan.resolveUsing(...). In more,
advanced use cases, classifiers have the ability via the plan to change
the morphers used and the output schema and to perform batch translations,
but these should be rare and are not documented here.

Time
~~~~

A discussion of the standardized time string that is used can be found at
`NETCONF date-and-time
<http://www.netconfcentral.org/modules/ietf-yang-types>`_

The output format can be described by the following regular expression::

\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[\+\-]\d{2}:\d{2})

The supported time input formats (that are convertible to the standard output
format above) are either epoch seconds or a local time that is missing the
time zone (common in Juniper devices).

Epoch seconds is a large integer that represents the number of seconds since
01/01/1970 in GMT.

The local time format is like the ISO 8601 format above, but has a dash
instead of the T and is missing the zone offset. An example would be
2018/06/17-14:36:00.

For routines that need a zone offset, the value can either be "UTC" or
a string with the format [+-]dd:dd, where d is a digit and the whole offset
is the hours:minutes off of GMT. An example is "+06:00" for US CST.

Note that without a time zone offset, a given time is ambiguous and cannot
be shifted to a known time zone. If an ambigous time is supplied to the
time normalization routines, an ambigous time is returned and can be
recognized by the offset "-00:00" per the NETCONF standard above.

XML vs JSON
~~~~~~~~~~~
The intent is to keep the features for handling of XML and JSON the same within Plastic.
But right now there are two features that are in JSON but not XML. The first is the use of
the array indexed variables [*] feature. The second is the allowing of non-scalar values for
defaults.

Configuration
~~~~~~~~~~~~~
Plastic can periodically poll the file system for changes to classifiers, morphers, and
schemas. This is considered a development feature and normally is disabled. It can be enabled
by supplying an integer polling delay value (in seconds) greater than 0. The value 0 will
disable the polling. Client applications of Plastic usually control this via a property
in a properties file.

Appendix
~~~~~~~~
This document can be converted to PDF using `rst2pdf
<https://github.com/rst2pdf/rst2pdf>`_

`RST syntax reference
<http://docutils.sourceforge.net/docs/user/rst/quickref.html>`_
