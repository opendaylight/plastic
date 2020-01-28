.. footer::

  *Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.*
  *This program and the accompanying materials are made available under the*
  *terms of the Eclipse Public License v1.0 which accompanies this distribution,*
  *and is available at http://www.eclipse.org/legal/epl-v10.html*

=======
Plastic
=======
No Coding Tutorial (for trouble-shooters and translation designers)
-------------------------------------------------------------------

*June 9, 2020*

.. contents:: Table of Contents

What Is Here
~~~~~~~~~~~~
Translating between text-based representations is a common problem, particularly in modeling domains.
The translation problem in its simplest form is to take one input of a known structure and convert it
to one output of a structure. The structure or shape of the input and output are known as its schema.

Plastic is a tool that supports very flexible specifications of translations. It handles both
declarative data-oriented specifications of translations and varying depths of imperative or code-based
specifications.

This document is structured as a Plastic tutorial, but contains information making it useful
as a user manual. If you read through and do the examples in here, you should plan on spending about
four hours. If you are a translation designer, you should go through this whole tutorial. If you are
a troubleshooter, you should go through the first main section "Plastic-Tutorial-1 - No Coding".

There is a companion document "Plastic-Authoring" that covers writing advanced logic for
translations.

Plastic Without Programming
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Chapter 1 - Rationale
^^^^^^^^^^^^^^^^^^^^^

Conventional Translation
++++++++++++++++++++++++
Text translation is an old problem. There are a lot of tools out there that can do a good job at this,
depending on your needs. Examples would be Apache Velocity, XML/XSD, or even ad hoc programming in
your favorite language (Python, Perl, etc.) (You could even use Jolt, which is spiritually a lot closer
to Plastic than anything else)

Most conventional translation systems

- require you to learn a specific template language to accomplish anything
- require a coder or scripter to make even the simplest changes
- obfuscate the shape (i.e., schema) of the data, making it harder to understand


If you opt for using a general-purpose programming language, then

- the schema often is rendered as nested if-else, switch, and try-catch and other control statements
- the representation is extremely brittle, making it likely for regressions and difficult to update

Translation by Intent
+++++++++++++++++++++
The biggest value statement for Plastic is to support "Translation by Intent". Colloquially,
"say what you want, not how to do it". This means that in the ideal circumstance, designers would
create translations using data, like JSON and XML, and would not do any coding. Using data to
describe translations is declarative.

Because many translation problems are more complex, designers might have to write a varying degree
of logic. Although Plastic supports this, there is an overt architectural goal of improving
Plastic to encourage less required programming from translation designers.

Plastic then

- allows changes in the field by non-programmers
- avoids schema obfuscation/brittleness from ad hoc nested ifs and try-catches
- segregates schemas by vendor/version to avoid regressions
- supports “pay-as-you-go” development cost that tracks translation complexity

Should you use Plastic?

- if you have programmers on hand ready to go, then no
- if you don't care how hard it is to understand your translations, then no
- if you are not particularly sensitive to regression breakages, then no
- if you have high throughput requirements, like less than 150 msec, then probably no

In all seriousness, Plastic is a tool. Like all tools, it is not a panacea for every problem.
Think about your problem space and choose your tool deliberately.

Chapter 2 - Translation Concepts
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Translation involves working with data. That data is usually structured, and the shape of that
structure is called its *schema*. The data is usually rendered in a data transport format like
JSON or XML.

The simplest translation involves morphing a single input to a single output. In this simple
scope, the designed translation takes an input of a known schema and *morphs* it to fit a known
output schema.

A more complex translation would involve looking at the incoming data (aka *payload*), figuring
out which *input schema* to use, then morphing the data into the shape defined by the *output schema*.

A more complex translation would involve breaking up a complex payload into multiple smaller,
less complex translations.

Plastic has means of configuring each translation beyond the usual incoming payload, the
input schema, and the output schema. A later section will cover these other optional mechanisms
called *morphers* and *classifiers*.

Chapter 3 - Plastic-In-A-Box
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. caution::

  You must have Java 8 installed on your machine to be able to run Java programs

There is a utility *PlasticRunner* to help translation designers to quickly understand the
effects of their changes. This utility is effectively a fully-featured, self-contained Plastic.
It can do everything a production version of Plastic can.

See the document Plastic-Runner for more details.

PlasticRunner was created to help speed up the schema, morpher, classifier writer's development
cycle. Without this utility, designers would have to access the the program in which Plastic is
embedded - not always possible or wise in a production environment.

It only takes a few seconds to start Plastic and make a translation pass. When it runs,
all of the output will be streamed to the console. Try running it now using::

  Plastic_runner.sh example.properties

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
input schema and output schema must exist somewhere in the morphers directory or you will get an
error. The payload file must exist by that path/name and must match the input schema of course.
The defaults file is optional - just leave it's value blank if you are not using it. Its format is a
simple JSON object, where the keys are the variable names and the values are the variable
values.

An example is

.. code-block:: json

  {
    "adminStatus": "UP",
    "mtu": "1500"
  }

The equivalent example, but for an XML-based defaults file would be

.. code-block:: XML

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

To obtain the latest utility, please contact the Lumina development team.

Chapter 4 - Simple Translation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The simplest translations require only three things: an input payload, and input schema definition,
and an output schema definition. In these simplest cases, *no coding is required*.

Concepts
++++++++
The *payload* is the data that is input to the translation and provides the source for all
values, and is usually JSON or XML. Plastic will use it to find values that the designer
thinks are important for this translation. Here is an example payload in JSON

.. code-block:: json

  {
    "admin-status": "UP",
    "min-mtu": "1500",
    "max-delay-msec": "10"
  }

Like the payload, the *input schema* is a data, and has a subset of the structure of the
input payload. The input schema shows Plastic what parts of the payload are to be carried
forward in the translation process into the output. You can think about this input schema as a
template. Here is an example input schema in JSON

.. code-block:: json

  {
    "admin-status": "${status}",
    "min-mtu": "${mtu}",
    "max-delay-msec": "${delay}"
  }

The *output schema* may be of a different format than the input schema. It too is a data file,
and it shows where to place the values that Plastic gathered from the payload. In many ways,
you can think about this output schema as a template. Here is an example in JSON

.. code-block:: json

  {
    "admin": {
      "overall-status": "${status}"
    },
    "network-element" : {
        "mtu": "${mtu}",
        "max-delay": "${delay}"
    }
  }

*This example is shown in Tutorial1 in the PlasticRunner directory.*

Variables
+++++++++

Both input and output schemas can have *variables* declared in them using the "dollar
variable syntax", like "${mtu}". You, as the schema designer, can choose any names you
want for the variables, as long as you avoid Plastic special characters, like {, },
=, or $.

Plastic will search the payload, using the input schema as the guide and will find
those values that correspond to the variables. It will *bind values* to those variables.
In the previous examples, the variable "mtu" will be bound to the value "1500" obtained
from the payload. So mentally you can think of a binding as a key and a value like this::

  mtu = 1500

If you know that the variable may not be present in the payload, you can give it a default
value as shown next::

  ${abc=123}

where **abc** is the variable name and **123** is the default value. The equals
sign separates the value from the variable name. The following example shows
**abc** with a default value of a blank string::

  ${abc=}

It is also possible to supply default values as part of the translate call itself, rather
than within the schema. More on that later.

Schemas
+++++++

Plastic has a schemas directory that holds input and output schemas used in mapping.
A "translate" call requires an input schema, an output schema, and a payload. The schemas
are either XML or JSON. An incoming payload must match the incoming schema, as it is not
possible to have an XML payload and a JSON input schema.

An example input or output schema looks like this

.. code-block:: json

  {
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
    "vlanId": "${vlanId}",
    "subIntfId": "${subIntfId}"
  }

This chapter was focused on "simple translation" which means that no explcit coding was is
required by you, the translation designer. Here are the bounds that describe when simple
translations are applicable:

- Input variables are **mapped directly** to output variables (they are not dropped or morphed)
- Variables may be **repeated** in the output schema
- Variables may be **combined** in the output schema
- Input variables are **optional** if they are given default values
- The output schema may have **hard-coded values** (like "adminStatus" above)
- Payloads can have extra data not found in the input schema and the **extra will be ignored**


Chapter 5 - Simple Errors and Warnings
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Plastic was designed to complain loudly when a translation does not go as intended. The
rationale for this is that you, the translation designer, failed to adequately describe the
schemas or how they relate to the payload. Almost all issues raised by Plastic could be
addressed by you understanding the translation problem with better fidelity.

The corollary to this is that a well-designed translation should have no warnings or
errors. If you see either one of these, you should carefully look at the context of the
translation (payload, schemas, etc) to understand why. You can find a comprehensive list of the
errors and warnings near the end of this document.

So what are typical warnings and errors?

Missing Inputs
++++++++++++++

Suppose you try to translate using a payload and schema like this

.. code-block:: json

  {
    "admin-status": "UP",
    "min-mtu": "1500"
  }

  {
    "admin-status": "${status}",
    "min-mtu": "${mtu}",
    "max-delay-msec": "${delay}"
  }

*This example is shown in Tutorial2 in the PlasticRunner directory.*

If you run this in PlasticRunner, you will see the following error

.. error::

  MissingInputsException ... PLASTIC-MISSING-IN: For (in-> [tutorial2-in/1.0/json]) (out-> [tutorial2-out/1.0/json]),
  the following input variables were not found on the incoming payload: [**delay**]

Plastic will complain because you told it to expect to find a variable value at the path "max-delay-msec"
that it could bind to the variable "delay". There was no such thing **in the payload** and there is no way for
Plastic to know what to do in this case, hence the error.

So you, the translation designer, did not account for an unexpected payload. There are several remedies to handle
a payload that might or might not have "max-delay-msec" in it. All of these remedies are discussed later in
detail. The high level description of these options are:

- you could modify the input schema by supplying a default value for "delay" in the schema itself
- you could supply a default value for "delay" as a paramter to the translation call
- you could write code in a morpher to handle this situation


Dangling Inputs
+++++++++++++++

Suppose you try to translate using a payload and input and output schemas like this

.. code-block:: json

  {
    "admin-status": "UP",
    "min-mtu": "1500",
    "max-delay-msec": "10",
    "max-jitter": 15
  }

  {
    "admin-status": "${status}",
    "min-mtu": "${mtu}",
    "max-delay-msec": "${delay}",
    "max-jitter": "${jitter}"
  }

  {
    "admin": {
      "overall-status": "${status}"
    },
    "network-element" : {
        "mtu": "${mtu}",
        "max-delay": "${delay}"
    }
  }

*This example is shown in Tutorial3 in the PlasticRunner directory.*

If you run this in PlasticRunner, you will see the following warning

.. warning::

  WARN ... - For (in:[tutorial3-in/1.0/json]) (out:[tutorial3-out/1.0/json]), the following input
  variables had no matching outputs: [**jitter**]

Plastic will complain because you told it that the variable jitter was important to bind a value to,
but then there was no such thing **in the output schema**. In other words, there was no place to put the
jitter value in the generated output.

So you, the translation designer, probably made an outright mistake in how you designed the input and output
schema - they just don't match up. This has nothing to do with the incoming payload. It is possible that
there is no mistake, as there might be morpher logic that uses jitter to calculate something else
that contributes to the outputs.

There are several remedies to handle this situation.

- you could just ignore the warning (not a very sanitary design)
- you could remove the jitter variable from the input schema
- you could add the jitter variable to the output schema
- if you use jitter in your morpher logic, you can tell Plastic not to worry about jitter at all

Dangling Outputs
++++++++++++++++

Suppose you try to translate using a payload and input and output schemas like this

.. code-block:: json

  {
    "admin-status": "UP",
    "min-mtu": "1500",
    "max-delay-msec": "10"
  }

  {
    "admin-status": "${status}",
    "min-mtu": "${mtu}",
    "max-delay-msec": "${delay}"
  }

  {
    "admin": {
      "overall-status": "${status}"
    },
    "network-element" : {
        "mtu": "${mtu}",
        "max-delay": "${delay}",
        "max-jitter": "${jitter}"
    }
  }

*This example is shown in Tutorial4 in the PlasticRunner directory.*

If you run this in PlasticRunner, you will see the following error

.. error::

  Exception ... DanglingOutputVariables: PLASTIC-DANGLING-OUT-VARS: For (in:[tutorial4-in/1.0/json])
  (out:[tutorial4-out/1.0/json]), the following output variables had no matching inputs: [**jitter**]

Plastic will complain because you told it that the variable jitter was important to to use its value,
but then there was no such variable in the **input schema**. There is no value for jitter to be used in
generating the output.

So you, the translation designer, probably made an outright mistake in how you designed the input and output
schema - they just don't match up. This has nothing to do with the incoming payload. It is possible that
there is no mistake, as there might be morpher logic that creates a jitter value that can be used in
the output.

There are several remedies to handle this situation.

- you could add the jitter variable to the input schema
- you could remove the jitter variable from the output schema
- if you create a jitter variable in your morpher logic, you can tell Plastic not to worry about jitter at all

Chapter 6 - Arrayed Variables
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Input
+++++

So far we have seen schemas that match payloads of a fixed structure or size. But there are
some translation problems that must deal with payloads of varying size. Specifically some
payloads contain lists of items and the length of those lists changes from payload to payload.

Consider the payload below, that has a list of IP addresses.

.. code-block:: json

  {
    "deviceName": "CXP-2501",
    "interfaceName": "TCP/1/0/24",
    "addresses": [
      {
        "address": "10.10.100.100",
        "prefix-length": "24"
      },
      {
        "address": "10.10.100.221",
        "prefix-length": "24"
      },
      {
        "address": "10.10.100.168",
        "prefix-length": "24"
      }
    ]
  }

Using our existing knowledge of defining schemas, we might be able to have this work.

.. code-block:: json

  {
    "deviceName": "${dName}",
    "interfaceName": "${iName}",
    "addresses": [
      {
        "address": "${addr-1}",
        "prefix-length": "${pref-len-1}"
      },
      {
        "address": "${addr-2}",
        "prefix-length": "${pref-len-2}"
      },
      {
        "address": "${addr-3}",
        "prefix-length": "${pref-len-3}"
      },
    ]
  }

What if the incoming payload has 6 addresses? Well, the variable binding would actually
work, because we know that Plastic doesn't care about extra content in the
payload - which applies to addresses 4, 5, and 6.

What if the payload has 2 addresses? Well, this would result in an error, because
Plastic would not find payload content to match for address 3.

Although this input schema sometimes matches the incoming payload, it is very brittle and
cannot match other common variants in the payload.

So how can Plastic deal with payloads that contain lists like the address list above?
Look at the schema below.

.. code-block:: json

  {
    "deviceName": "${dName}",
    "interfaceName": "${iName}",
    "addresses": [
      {
        "address": "${addr[*]}",
        "prefix-length": "${pref-len[*]}"
      }
    ]
  }

The new syntax shows "[*]" added to the variable names. Go back to our payload where there
are 3 addresses in the list. Once Plastic does the variable binding to the payload,
there will be an internal bound variables list like this::

  addr[0] = "10.10.100.100"
  addr[1] = "10.10.100.221"
  addr[2] = "10.10.100.168"

So you can see there are 3 variables using the "arrayed variable" syntax. The "addr" is
the name used from the schema. Arrayed variables start at index 0 and go up from there.
If the address list was empty on the payload, it is not an error - there would just be
no bindings for "addr".

Output
++++++

So far we have been talking about how arrayed variables have their values bound from the
input schema and payload. So how can they be used in the output schema?

Here is an example output schema

.. code-block:: json

  {
    "dev-name": "${dName}",
    "interface-name": "${iName}",
    "objects": [
      {
        "subnet": "${addr[*]}/${pref-len[*]}"
      }
    ]
  }

And using our original payload, here is the final output of the translation.

.. code-block:: json

  {
      "dev-name": "CXP-2501",
      "interface-name": "TCP/1/0/24",
      "objects": [
          {
              "subnet": "10.10.100.100/24"
          },
          {
              "subnet": "10.10.100.221/24"
          },
          {
              "subnet": "10.10.100.168/24"
          }
      ]
  }

*This example is shown in Tutorial5 in the PlasticRunner directory.*

Some caveats

- If your schema is dealing with multiple arrays, then all must be the same length (or have default values)
- If your schema is using nested arrays, see the next chapter
- The arrayed variables feature has not been implemented for XML

Note that there is more advanced authoring support, not covered here, that allows writing
code that can conveniently manipulates these arrayed variables (look for MoArray).

In all of the previous examples, the use of arrayed variables in the output schema assumed that there was only
a single element to expand. But you can have more complex usage where you have multiple elements to expand
per iteration of the asterisk binding. Consider the example below, and assume that there are 100 elements bound
to the asterisk.

.. code-block:: json

  {
    "my-objects": [
      {
        "address": "${addr[*]}"
      }
    ]
  }

The "my-objects" array is the parent collection for the asterisk and it has only a single object (which has an
"address") to expand. When the output is created, the "my-objects" array will have 100 elements. But the output
schema below will work too, resulting in 300 elements!


.. code-block:: json

  {
    "my-objects": [
      {
        "address": "${addr[*]}"
      },
      {
        "length": "${pref-len[*]}"
      },
      {
        "name": "${name[*]}"
      }
    ]
  }


Chapter 7 - Nested Arrayed Variables
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Welcome one of the most powerful features in Plastic. Complex payloads like those found in OpenConfig deal
with nested array stuctures. Up to this point, we have been talking about **singly dimensioned** arrays. These
can be full of scalar values or complex object values. Support for nested arrays means that an array can
have embedded arrays of things to. Nested arrays are also called **multiply dimensioned**.

An array has a size that usually isn't known until an arriving payload is parsed. The array indices are
zero-based (like most programming languages assume). So an array of size N will have individual indices of
0 ... N-1. Iterating is a concept of processing each element of an array, one after the other, and the index
value identifies the iteration.

The enclosing array is a **parent array** that encloses one or more **child arrays**. The children are all sibling
arrays. Any given array can be a parent, a child, or both all at the same time. The parent-child relationship
is important because generating a new element for a parent array means that a new, fully complete child array needs
to be generated per parent array element. More on this later.

The whole intent behind this feature, indeed of Plastic itself, is the **decoupling** of incoming structure from
outgoing structure. In terms of nested arrays, the nesting that is found in the incoming data can be expressed
differently in the outgoing data. The nestings do not need to match at all, and Plastic gives a way to express
that without writing code.

Flattening
++++++++++
One of the important ways that an output structure can be expressed differently than the input structure is
called **flattening**. This can be done via **partial flattening** or **full flattening**. A singly dimensioned
array is already as flat as it can be, so flattening it makes no sense. But consider an nested array structure
like this (3x3)::

    [
        [ a, b, c ]
        [ d, e, f ]
        [ g, h, i ]
    ]

Since this is two levels of nesting, it can be flattened to a single level like this (1x9)::

    [
        a, b, c, d, e, f, g, h, i
    ]

Here is a 3 dimensional array (2x2x2)::

    [
        [
            [ a, b ]
            [ c, d ]
        ]
        [
            [ e, f ]
            [ g, h ]
        ]
    ]

Here is partly flattened result (2x4)::

    [
        [
            a, b, c, d
        ]
        [
            e, f, g, h
        ]
    ]

And here is the fully flattened result (1x8)::

    [ a, b, c, d, e, f, h, h ]

In summary, flattening is an output concept. It is reducing the level of input nesting present in the input data.

Rectangular Indices and Holes
+++++++++++++++++++++++++++++

Below is a 3x3 chunk of nested array data shown as JSON. It has rectangular indices because it is
a true 3x3. That is, there are no missing values (not the same as blank values).

.. code-block:: json

    [
        [ "a", "b", "c" ],
        [ "d", "e", "f" ],
        [ "g", "h", "i" ]
    ]

Unfortunately, when dealing with real payloads, things are not so clean. Here is an example of a
3x3 but it does not have rectangular indices.

.. code-block:: json

    [
        [ "a", "b", "c" ],
        [ "d", "e" ],
        [ "f" ]
    ]

In fact, the number of child arrays defines the first indice and the length of the longest child
array defines the second indice. So below is also a non-rectangular 3x3

.. code-block:: json

    [
        [ "a", "b", "c" ],
        [ "d" ],
        [ "f" ]
    ]

The missing values, called **holes** in this document, can be anywhere. Plastic can deal gracefully
holes without error. But if a particular output value is a mixing of two arrayed variables, like
this::

    "${a[*]}/${b[*]}"

and one of them is missing, say there is no "b[2]" to match up against an "a[2]", then it is an error.
You can supply a default value **in the output schema** like this to supply the missing value::

    ${a[*]}/${b[*]=ouch}

Rule::

    If your array data is not rectangularly indexed, you can use default values in the output
    schema to rectangularize it. Note that previous versions of Plastic only recognized default
    values in the input schema.

Syntax
++++++

So a single dimensioned array might use a variable syntax like this, valid on both the input and
output schemas::

   $a[*]

A multiple dimensioned array might use a variable syntax like this, valid on both the input and
output schemas::

   $a[*]           <- dimension = 1
   $a[*][*]        <- dimension = 2
   $a[*][*][*]     <- dimension = 3
   $a[*][*][*][*]  <- dimension = 4

In addition, there is another array index syntax using a caret, that is applicable only for
output schemas. It is used indicate that the index should be borrowed from the enclosing
parent array. Here are examples::

   $a[*]           <- first (and only) dimension comes from bound data
   $a[^][*]        <- first dimension borrowed from parent, second from bound data
   $a[^][^][*]     <- first 2 dimensions borrowed from parent, last from bound data
   $a[^][^][^][*]  <- first 3 dimensions borrowed from parent, last from bound data
   $a[^][^][*][*]  <- first 2 dimensions borrowed from parent, second 2 from bound data

Here are some ILLEGAL examples of output arrayed variable syntax::

   $a[^]           <- No!
   $a[*][^]        <- ???
   $a[^][*][^]     <- gibberish
   $a[*][^][^][*]  <- WTF

Rule::

    Input schema:
        Use abc[*]...[*] on the input schema (no other syntax is valid)

Rule::

    Output schema:
        Use abc[^]...[^][*] to avoid flattening (the normal case)
        Use abc[^]...[^][*]...[*] to partially or fully flatten

Iterators
+++++++++

A schema iterator is a set of values that describe how many times an element needs to be repeated
in an array. I is basically an optional name, a set of dimensions, and a set of counters. As a Plastic
user, iterators are conceptual only. You do not need to deal with them directly. But you will see
some ensuing examples that show iterator values to help you understand what is happening on the input
aka binding side and on the output aka expansion side.

Here is an example of an iterator you might see if you wrote morpher logic and dumped the input map::

    _[abc[*][*]]  =  [3,2]

The key is the left of the = and the value is the right of the =. The name is everything between
the _[...], which is abc plus the indices. The value is 3x2, which is the dimensions of the bound
data.

Examples
++++++++

Thing may be a bit murky, so lets dive into examples to clear things up. These assume that you are
already familiar with most Plastic concepts. Note that you will see some internal bookkeeping iterators
in the following examples

Example: Independent Sibling Arrays
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Payload...

.. code-block:: json

    {
        "abc": [ "aa", "bb", "...", "zz" ],
        "colors": [ "red", "green", "blue" ]
    }

Input schema...

.. code-block:: json

    {
	    "abc": [ "${abc[*]}" ],
	    "colors": [ "${colors[*]}" ]
    }

Internal bindings...

.. code-block:: Java

    abc[0] = aa
    abc[1] = bb
    ...
    abc[25] = zz

    _[abc[*]] = [26]

    colors[0] = red
    colors[1] = green
    colors[2] = blue

    _[colors[*]] = [3]

Output schema...

.. code-block:: json

    {
	    "foo": [ "${abc[*]}" ],
	    "bar": [ "${colors[*]}" ]
    }

Output...

.. code-block:: json

    {
	    "foo": [ "aa", "bb", "...", "zz" ],
	    "bar": [ "red", "green", "blue" ]
    }

Example: Mixing Input Arrays into a Single Output Array
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Payload...

.. code-block:: json

    {
	    "aaa": [ "aa", "bb", "cc" ],
        "colors": [ "red", "green", "blue", "purple" ]
    }

Input schema...

.. code-block:: json

    {
	    "aaa": [ "${aaa[*]}" ],
	    "colors" [ "${colors[*]}" ]
    }

Internal bindings...

.. code-block:: Java

    aaa[0] = aa
    aaa[1] = bb
    aaa[2] = cc

    _[aaa[*]] = [3]

    colors[0] = red
    colors[1] = green
    colors[2] = blue
    colors[3] = purple

    _[colors[*]] = [4]

Output schema...

.. code-block:: json

    {
	    "foobar": [ "${aaa[*]=xx}/${colors[*]=gray}" ]
    }

Output...

.. code-block:: json

    {
	    "foobar": [ "aa/red", "bb/green", "cc/blue", "xx/purple" ]
    }

The "foobar" array in the output schema is constructed from two arrayed variables "aaa" and
"colors". If the length for aaa is not the same as the length for colors, then this would
result in an error, unless there are defaults present.

Example: Two Deep Multi-dimensional Example
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(This is a shared input and bindings used to support several ensing examples)

Payload...

.. code-block:: json

    {
      "aaa": [
          {
              "id": "1",
              "bbb": [ "a", "b", "c" ]
           },
          {
              "id": "2",
              "bbb": [ "d", "e" ]
           },
          {
              "id": "3",
              "bbb": [ "f" ]
           },
           {
              "id": "4",
              "bbb": [ ]
           }
        ]
    }

Input schema...

.. code-block:: json

    {
      "aaa": [
          {
             "id": "${id[*]}",
              "bbb": [
                 "${bbb[*][*]}"
              ],
           }
       ]
    }

Internal bindings...

.. code-block:: Java

     id[0] = 1
     id[1] = 2
     id[2] = 3
     id[3] = 4

     _[id[*]] = [4]

     bbb[0][0] = a
     bbb[0][1] = b
     bbb[0][2] = c
     bbb[1][0] = d
     bbb[1][1] = e
     bbb[2][0] = f

     _[bbb[^][*]] = [4,3]

Note::

   This example shows that you cannot even count on rectangular matrices (ie, bbb is not 4x3 with 12 members).
   And because of this, we may have shortened ranges (they must be dealt with). Worse, they actually can be real
   holes and can be at any level of dimension. So even though there are only 6 entries for bbb, we have to
   iterate over all 12 possibilities to be sure to cover everything.

Example: Flattening
~~~~~~~~~~~~~~~~~~~

(This example uses the inputs and bindings from above)

Output schema...


.. code-block:: json

    {
      "foo": [
          "${id[*]}"
      ],
      "bar": [
          "${bbb[*][*]}"
      ]
    }

Output...

.. code-block:: json

    {
      "foo": [ 1, 2, 3, 4 ],
      "bar": [ "a", "b", "c", "d", "e", "f" ]
    }

You know this data was being flattened because you can see the "[*][*]" in the output schema on "bbb".
Flattening occurs when the number asterisks greater than 1.

Example: Non-flattening
~~~~~~~~~~~~~~~~~~~~~~~

(This example uses the inputs and bindings from above)

Output schema...

.. code-block:: json

    [
      {
          "foo": [
              "${id[*]}"
          ],
          "bar": [
              "${bbb[^][*]}"
          ]
      }
    ]

Output...

.. code-block:: json

    [
      {
          "foo": [ 1, 2, 3, 4 ],
          "bar": [ "a", "b", "c" ]
      },
      {
          "foo": [ 1, 2, 3, 4 ],
          "bar": [ "d", "e" ]
      },
      {
          "foo": [ 1, 2, 3, 4 ],
          "bar": [ "f" ]
      },
      {
          "foo": [ 1, 2, 3, 4 ],
          "bar": [ ]
      },
    ]

New syntax is [^] which means that this dimension is going to come from a parent array. A
valid syntax is now extended to be zero or more [^] followed by one or more [*].

Chapter 8 - Pattern Matching
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Schema designers often need to parse apart an input field to find a substring or two to assign as variable
values. If the needs are simple, this can be done in the schema rather than writing any morpher logic.
This capability uses the wildcarding feature.

The wildcarding feature may be used only on the input schema. It is a way to match against an input string and
to assign substrings to particular variables. This is a distant cousin of Java's regex for those of you who are
versed in programming.

Lets look at an example to explain the feature. Here is an example input value that might be found on an incoming
payload.

.. code-block:: Java

  "one/two three"

Here is an example showing usage in an input schema used to match against that payload.
Once the payload is parsed, then ${abc} will be "one" and ${def} will be "two three".

.. code-block:: Java

  "|${abc}/${def}|"

Things to note here:

- Wildcarding must begin and end with a vertical bar | which represent the start and end of the candidate string
- There may be one or more variables defined within that wildcard expression
- Variable values land on "word boundaries", including numbers, punchtuation, whitespace, etc
- There can be literal character values like the slash / above that must match exactly with the candidate string
- If the candidate does not match, then the entire translation results in an error

.. code-block:: Java

  "|*${abc}/${def}*|"

  "one two/three four"

  ${abc} = "two"
  ${def} = "three"

In the above example, you can see the usage or the wildcard aka asterisk. The asterisk is a stand-in for any number
of characters in the string that can be skipped (not part of the variable value). The asterisk is greedy,
meaning that it matches as much of the string as possible. The literal slash / matches the one in the payload and
kind of "anchors" the wildcard expression to the candidate string.

.. code-block:: Java

  "|*${abc} ${def}*|"

  "one two/three four"

  ${abc} = "three"
  ${def} = "four"

The above example uses the space as an "anchor". Because asterisks are greedy and because there are a couple of
spaces that can be matched against, the wildcard logic matches the last one (rather than the first one).

.. code-block:: Java

  "|* ${abc}/${def} *|"

  "one two/three four"

  ${abc} = "two"
  ${def} = "three"

In the above example, the wildcard expression has two literal spaces which match exactly the spaces in the
candidate string, anchoring it against the candidate, so that the wanted values are "two" and "three".

.. code-block:: Java

  "|${abc} *|"
  "one two/three four"
  ${abc} = "one"

In the above example, "abc" matches to the first word "one". Although asterisks are greedy, the variable values
are not. So the literal space anchors to the first space in the candidate.

.. code-block:: Java

  "|* ${abc}|"
  "one two/three four"
  ${abc} = "four"

In the above example, the greedy asterisk matches everything till it hits the literal space, leaving the
variable value to be the last word.

You can use the command line runner and a very simple input/output schema to test our your wildcard expressions.


Addenda
^^^^^^^

Over-specification and the Wrapping Use Case
++++++++++++++++++++++++++++++++++++++++++++

A fairly common use case for translation is to take a portion (or the full content) of the incoming
payload produce a wrapped verison of it as output. There are a couple of ways to approach this problem.

The simplistic way is shown below in the input and output schemas.

.. code-block:: json

  {
    "network-element" : {
      "max-jitter": "${jitter}",
      "min-mtu": "${mtu}",
      "max-delay-msec": "${delay}"
    }
  }

  {
    "ne-configuration": {
      "network-element" : {
          "max-jitter": "${jitter}",
          "min-mtu": "${mtu}",
          "max-delay-msec": "${delay}"
      }
    }
  }

Now compare the solution above to the following input and output schemas.


.. code-block:: json

  {
    "network-element" : "${ne-stuff}"
  }

  {
    "ne-configuration": {
      "network-element" : "${ne-stuff}"
    }
  }

The latter is superior in every way.
* It is shorter and easier to understand.
* It is immune to changes in the schema under "network-element".
* It is a less complex task because the extra variable values do not need to be individually handled.

The first simplistic attempt is an example of over-specification.

Appendix
~~~~~~~~
This document can be converted to PDF using `rst2pdf
<https://github.com/rst2pdf/rst2pdf>`_

`RST syntax reference
<http://docutils.sourceforge.net/docs/user/rst/quickref.html>`_




To continue the tutorial, see the separate part 2.
