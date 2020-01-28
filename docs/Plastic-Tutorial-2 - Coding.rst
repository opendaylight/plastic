.. footer::

  *Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.*
  *This program and the accompanying materials are made available under the*
  *terms of the Eclipse Public License v1.0 which accompanies this distribution,*
  *and is available at http://www.eclipse.org/legal/epl-v10.html*

=======
Plastic
=======
Coding Tutorial (for programmers)
---------------------------------

This is the second part of the tutorial. For the "no coding" tutorial, see part 1.

Programming and Plastic
~~~~~~~~~~~~~~~~~~~~~~~

The goal of using Plastic for your mapping needs is to do all the work in the schemas and not write any
code. If you have to write code, you want to minimize how much you write. Plastic features and support
logic are constantly being improved to help support this goal. This section will give you a deeper understanding
of how to manipulate your incoming data.

Chapter 7 - Default Values
^^^^^^^^^^^^^^^^^^^^^^^^^^

Sometimes payloads change content. Sometimes they are missing values. To help manage this variation in your
payloads, Plastic has a concept of default values. Normally if your input schema defines a variable
like ${abc} and that is not present in the payload, it is an error. But with default values, you can tell
Plastic what value to use for that variable in the case that it cannot be located within the incoming
payload.

Default values can come from three places

- Static default values embedded in the input schema itself
- Static defaults supplied via a hash map on call to translate itself
- Explicitly supplied default values in morpher logic

Typically default values are scalar values, like a number or a string. But generally, they can be more
complex values, like arrays or objects. They must be string scalars if embedded in the input schema, but they
can be anything via the other two methods.

Here is an example input schema showing a default value assigned to delay of "15". So if the payload
is missing "max-delay-msec", then Plastic will use "15" as its value and will not generate an error.
This value is interpreted as a string in this example.

.. code-block:: json

  {
    "admin-status": "${status}",
    "min-mtu": "${mtu}",
    "max-delay-msec": "${delay=15}"
  }

A common usage is to set a blank value as the default, as show in the example input schema below. Here the
status has no value after the equals sign, so blank is used.

.. code-block:: json

  {
    "admin-status": "${status=}",
    "min-mtu": "${mtu}",
    "max-delay-msec": "${delay}"
  }

Using default values for arrayed variables should be very rare. It is not a mistake but it is likely going
to confuse anyone looking at your schema. This is particularly true if you have an arrayed variable and
**all** of the values have defaults. The reason these normally won't be used is that Plastic handles all
of the work, regardless of how many members of the array are in the payload. If the array in the payload is
empty, that is fine and is not an error. You should rarely if ever need to tell Plastic that if the
array is empty, please bind a single member.

Here is an example of such a **confusing schema**

.. code-block:: json

  {
    "deviceName": "${dName}",
    "interfaceName": "${iName}",
    "addresses": [
      {
        "address": "${addr[*]=}",
        "prefix-length": "${pref-len[*]=}"
      }
    ]
  }

This one is "normal" because it does not seem to imply that a new array element will be created if the array
is empty.

.. code-block:: json

  {
    "deviceName": "${dName}",
    "interfaceName": "${iName}",
    "addresses": [
      {
        "address": "${addr[*]}",
        "prefix-length": "${pref-len[*]=}"
      }
    ]
  }

Defaults can be programmatically passed into the translate() call via a stringified map of values.
A JSON and XML version follow.

.. code-block:: json

  {
    "adminStatus": "UP",
    "mtu": "1500"
  }

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

Chapter 8 - Pipeline
^^^^^^^^^^^^^^^^^^^^

Plastic has a pipeline of steps that it uses for each transformation. This series of steps is
always followed, but some steps are optional. These optional steps depend on the presence of logic
plugins called classifiers and morphers.

As an advanced translation designer, you need to understand this sequence if you are writing
classifiers or morphers. Classifiers, discussed in detail later, have a single callback hook.
Morphers, also discussed in detail later, have optionally up to three different callback hooks.
This allows the designer to pick an early, middle, or late participation in the Plastic
pipeline.

Detailed Pipeline Steps
+++++++++++++++++++++++

This list of steps is really for the curious. Skip to the next subsection for an abstracted view of
the pipeline.

#. Create input in memory by parsing the payload
#. **Callback classifier** to disambiguate input schema name
#. Load optional input morpher and output morpher
#. Parse input schema and locate $ input variables
#. Bind values to $ input variables by walking payload
#. If any default values as part of translate call, bind their values
#. **Callback morpher** (tweakInputs()) for input morpher then output morpher
#. Parse output schema and locate $ output variables
#. Bind values to $ output variables by using values from $ input variables
#. **Callback morpher** (tweakValues()) for input morpher then output morpher
#. Instantiate the output tree using the output schema as a template
#. Inject $ output values into the output tree
#. **Callback morpher** (tweakParsed()) for input morpher then output morpher
#. Validate by looking for dangling/unused $ input and $ output variables
#. Serialize and emit the output

Simple Pipeline Steps
+++++++++++++++++++++

Although the pipeline steps above are detailed, you, as a translation designer, only need to
keep a simplified version in mind, namely

#. Parse everything
#. **Optional callback to classifier**
#. Bind all input values
#. **Optional early callback to morpher**
#. Bind all output values
#. **Optional middle callback morpher**
#. Create first draft of output
#. **Optional late callback to morpher**
#. Generate output

Chapter 9 - Morphers
^^^^^^^^^^^^^^^^^^^^
This chapter covers the following:

- What are morphers?
- How do morphers fit into the Plastic pipeline?
- How can I automatically associate mophers with a given translation?

Plastic has a morphers directory that holds optional groovy classes, called morphers,
that are used to do more sophisticated kinds of mapping. They must be named to match either
the full input schema name, full output schema name, or both, except that they end in .groovy
rather than .xml or .json. You can find example morphers in the morpher directory in the
Plastic installation area.

A simple morpher, showing the most commonly used callback hook, looks like this. Only one of
the callback hooks needs to be supplied, and in this case, it is "tweakValues".

.. code-block:: java

  import org.opendaylight.plastic.implementation.BasicMorpher

  class MySpecialMorpher extends BasicMorpher
  {
      void tweakValues(Map ins, Map outs) {
          ...
      }
  }

A full signature morpher, showing all three of the callback hooks, looks like this

.. code-block:: java

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

The morpher must have at least one of those three methods to be well-formed and accepted by
Plastic.

Morphers and support logic are described in more detail in a companion document Plastic-Authoring.

tweakInputs()
+++++++++++++

The **tweakInputs()** morpher method allows derived morphers the earliest access in the pipeline. The
actual type signature is

.. code-block:: java

  void tweakInputs(Map inputs, Object payload)

The **inputs** map is keyed by the variable names found in the input schema and the values are either
found in the payload or from any defaults. Example variable names a.k.a. keys, might be "jitter",
"address[0]", "address[1]", etc.

The **payload** is the in-memory parsed content. It originally arrived to the translate call as a string,
but now it is fully parsed per the transport format.

.. important::

  It is not possible to have a uniform in-memory model for parsed data without loss of information.
  For JSON, the in-memory form is Java/Groovy maps, lists, and scalars.
  For XML the in-memory form is Java/Groovy XML Node structures.

The logic you write for this method typically might be used for

- input value validation
- calculating an input value based on examining the payload

Although that in-memory payload is used in the remaining pipeline steps, it is not recommended that
you surgically modify it. If you need to modify a payload, a classifier is a better place to park
that logic.

tweakValues()
+++++++++++++

The **tweakValues()** morpher method allows derived morphers the mid-point access in the pipeline. The
actual type signature is

.. code-block:: java

  void tweakValues(Map inputs, Map outputs)

The **inputs** map is keyed by the variable names found in the input schema and the values are either
found in the payload or from any defaults. Example variable names a.k.a. keys, might be "jitter",
"address[0]", "address[1]", etc. The inputs should be considered as read-only (i.e., not for modification).

The **outputs** map is mostly a copy of the input map. It can have more entries, because the output
schema might define variables not found in the input. In these cases, the key will have a null
value. The outputs are freely available for creation and modification. It should be rare to need to
delete an entry in the outputs.

This is really one of the best methods to put morpher logic because that logic is completely independent
of both the input and output formats. So changes to the shape of the schemas is less likely to impact
this logic. This is also the method that should be used most by beginner designers.a

The logic you write for this method typically might be used for

- input value validation
- calculating an output value based on examining the inputs

tweakOutput()
+++++++++++++

The **tweakParsed()** morpher method allows derived morpher that last possible access in the pipeline
prior to emitting of the final output payload. The actual type signature is

.. code-block:: java

  void tweakParsed(Object inTree, Object outTree)

The **inTree** is the in-memory content for the incoming payload. It should be considered read-only,
as changing it will have no effect on the translate results.

The **outTree*** is the in-memory content for the outgoing response. It is fully available for
manipulation. Keep in mind that logic that manipulated the outgoing response is very sensitive
to changes from the output schema (which defines the shape of the outgoing response). For this reason,
you should avoid writing logic here if it can be done other ways.

Chapter 10 - Morpher Examples
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Here are three examples that show usage of each of the three callbacks that are supported for
morphers. All are found in the specified tutorial files in the PlasticRunner directories.

- Tutorial 6 shows the use of tweakInputs() to do input validation
- Tutorial 7 shows the use of tweakValues() to create a URL encoded output variable
- Tutorial 8 (A and B) show the use of tweakParsed() to alter the output structure based on an input value

Chapter 11 - Libraries
^^^^^^^^^^^^^^^^^^^^^^
Morphers can use shared code by putting the shared code in the "lib" folder. The code should
be a file with a package statement at the top and a class definition. The package name can
be arbitrary but must follow the usual Groovy/Java name rules. The file can be in any arbitrary
directory structure since Plastic will recurse to load the files.

You **must** use a package statement. Leaving the package statement off puts the class in the
default package and there is no way to import that class into a non-default package.

Chapter 12 - Classifiers
^^^^^^^^^^^^^^^^^^^^^^^^

Classifiers are another optional plugin that changes the behavior of a translation. Classifiers are
very useful to manage two circumstances. The first is to handle the case of examining an incoming
payload to decide which schema to use. Recall that up to now, translation designers could handle
translations from one known schema to another known schema. By using a classifier, translations can
now occur from an unknown schema (now resolved at run time) to a known schema.

The second classifier, called a planning classifier, will be discussed in a later section of
this document.

Normally a translate call has an input schema name, version, and type like "my-schema",
"1.0", and "json". These are put together to form a file name like "my-schema-1.0.json"
and this file can live anywhere under the schemas directory.

When an schema name cannot be determined without examining the contents of the incoming
payload, then writing a classifier is necessary. To invoke a classifier, make the translate
call using an input like "my-schema-${EVENT_ID}", "1.0", "json". Plastic will see the
"${EVENT_ID}" and will look everywhere inside the classifiers directory for a file named
EVENT_ID.groovy.

A very simple classifier looks like this

.. code-block:: java

  import org.opendaylight.plastic.implementation.SimpleClassifier

  class GoodClassifier extends SimpleClassifier {
      String classify(Object parsedPayload) {
          "my-schema-a"
      }
  }

Note that the classify() method is called with a payload to examine.

.. important::

  The parsed payload is an in-memory representation of the parsed string payload.
  For JSON, the in-memory form is Java/Groovy maps, lists, and scalars.
  For XML the in-memory form is Java/Groovy XML Node structures.

*This example is shown in Tutorial9 (A and B) in the PlasticRunner directory.*

Chapter 13 - The N-to-1 Problem
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A recurring usage pattern with translations is call "the N-to-1 problem". In this usage, there are
N input schemas that need to map to a single output schema. This configuration requires a classifier
to pick the input schema (by looking at the payload). It might use N input morphers and/or
possibly 1 output morpher.

Don't be confused about input versus output morpher. A translation can have 0, 1, or 2 morphers that
are automatically picked up by Plastic. There is nothing different about an input or output
morpher. The only difference is ordering of execution among morphers if there are multiple morphers.
An input morpher, if present, has its name deduced from the input schema name. An output morpher,
if present, has its name deduced from the output schema. Plastic will order their execution
so that any input morpher is called before any output morpher. That's it.

So why would you choose to name a morpher based on either the input versus the output schema name.
It comes down to what the morpher logic is doing. Sometimes it is a lot shorter to have logic that
knows what the specific input is - so in these cases, use a morpher that is named after the input
schema. If the logic is not sensitive to the input schema then you can use an output morpher. In
the N-to-1 case, the output morpher ends up being shared no matter which input payload arrives.

So lets look closer at the **Tutorial 9 example**. You can run the A and B versions now in
PlasticRunner. This is an example of a 2:1 translation problem. In this example, there are two
input schemas A and B. There is a classifier that picks between the two input schemas by examining the
payload. There is a shared output morpher and a single output schema. It would be worth your time to
study this example.

Chapter 14 - Planning Classifiers
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

TODO: consider moving this chapter to "Plastic-Advanced" which will subsume "Plastic
Authoring"

So far you have seen simple classifiers, which are responsible for examining an incoming payload
and returning the name of the matching input schema. There is a second kind of classifier called
a planning classifier that can do this and much more.

A planning classifier can be used to
- examine an input payload and to determine the input schema
- modify in the incoming payload (to regularize it, for example)
- override the usual input and/or output morphers to include other arbitrary morphers
- break up a large translation into sub-translations

The cost of all this flexibility is that the logic of this classifier is more complex, so you
need to know more about what is happening.

Translation Plans
+++++++++++++++++

So what is a translation plan? In its simplest incarnation, a translation plan is

- an input schema name
- an output schema name

This simplest plan just tells Plastic to take the incoming payload, match it
using the input schema, then emit the results using the output schema. A slightly
more complext translation plan is

- an input schema name
- (an input) morpher
- an output schema name

or maybe

- an input schema name
- (an output) morpher
- an output schema name

This plan tells Plastic to take the incoming payload, match it using the input
schema, invoke a morpher, then emit the results using the output schema. The fully
generic translation plan would be

- an input schema name
- (an input) morpher
- any number of other morphers
- (an output) morpher
- an output schema name

So if a classifier is used to construct such a plan, it can alter schema names, it
can replace the deduced input morpher and output morphers, it can add arbitrary
morphers, and it can aribrarily reorder the morphers.

This translation plan concept will become clearer in the discussion of planning
classifiers next.

Planning Classifiers
++++++++++++++++++++

Till now you have been using simple classifiers. But behind the scenes, Plastic wraps
any simple classifier with an adapter, so actually planning classifiers are the only ones
actually used in translations.

Below is an example that shows using a planning classifier as a simple classifier. This is
more of a curiosity than anything else. It is here just so you see how a simple classifier
relates to a planning classifier.

.. code-block:: java

  import org.opendaylight.plastic.implementation.Schema
  import org.opendaylight.plastic.implementation.PlanningClassifier
  import org.opendaylight.plastic.implementation.TranslationPlanLite
  import org.opendaylight.plastic.implementation.VersionedSchemaParsed
  import org.opendaylight.plastic.implementation.author.Plans

  class ClassifierAdapter extends PlanningClassifier {

      @Override
      TranslationPlanLite classify(Schema parsedPayload, TranslationPlanLite plan) {
          plan.resolveUsing(classify(parsedPayload.getParsed()));
          return plan;
      }

      String classify(Object parsedPayload) {
          "my-schema-a"
      }
  }

Below is an outline for a planning classifier. You can see how it sets up a plan using
hard-coded schema names. There is support to avoid hard-code names, but it is not shown
here.

.. code-block:: java

  import org.opendaylight.plastic.implementation.Schema
  import org.opendaylight.plastic.implementation.PlanningClassifier
  import org.opendaylight.plastic.implementation.TranslationPlanLite
  import org.opendaylight.plastic.implementation.VersionedSchemaParsed
  import org.opendaylight.plastic.implementation.author.Plans

  class AnExamplePlanningClassifier extends PlanningClassifier {
      TranslationPlanLite classify(Schema versionedSchemaParsed, TranslationPlanLite plan) {
          TranslationPlanLite parent = Plans.newParent(
                  Plans.asSchema("batch-in", "1.0", "json"),
                  Plans.asSchema("batch-in", "1.0", "json"));
          parent
      }
  }

We are not going to do anything more in this section with planning classifiers, because the
compelling use case for planning classifiers involves batching, which comes next.

Chapter 15 - Batching and Parallelism
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Welcome to the most complex part of Plastic. Batching is the feature that makes this complexity
worth the cost. So what is batching? Batching is a means of breaking up a single, complex translation
into many smaller simpler translations that can run in parallel.

Breaking up a translation involves establishing a parent translation plan and one or more child
translation plans. Basically the classifier points each child to a portion of the payload and
put them under the stewardship of the parent. This parent plan is handed to Plastic, where
all of the children are run in parallel. The maximum parallelism is the "width" of the host (CPUs or
VCPUs), so the process runs as constrained parallelism.

Simple Example
++++++++++++++

Consider the following payload, where we have an array of messages of various types. Translating this
has some complexities. The first is that the number of elements in the message list can vary from
payload to payload. The second is that the message "types" can vary. If you try to do a code-only
solution to this, you will end up with schema details embedded in your code.

If you consider breaking this up into multiple schemas, this can become quite tractable. Namely,
have schemas for:

* each individual message type
* the parent structure that contains the list of messages

.. code-block:: none

  {
    "msglog": {
      "messages": {
        "message": [
          {
            "msg-type": "A",
            "property-A": "red"
          },
          {
            "msg-type": "B",
            "details-B": {
               "color": "red",
               "size": 3
            }
          },
          {
            "msg-type": "C",
            "body": {
               "contents": {
               }
            }
          },
          ...
          A,
          A,
          B,
          A,
          C,
          ...
        ]
      }
    }
  }

In the example above, there would be an input/output schema for the parent, and an input/output schema
for each message type, resulting in 2 + 2*3 = 8 very simple schemas.

Below is an example planning classifier that shows usage of child translations to get work like this done.

.. code-block:: java

  package acme

  import org.opendaylight.plastic.implementation.Schema
  import org.opendaylight.plastic.implementation.PlanningClassifier
  import org.opendaylight.plastic.implementation.TranslationPlanLite
  import org.opendaylight.plastic.implementation.VersionedSchemaParsed
  import org.opendaylight.plastic.implementation.author.BetterJson
  import org.opendaylight.plastic.implementation.author.Plans

  class BatchMsgClassifier extends PlanningClassifier {

      TranslationPlanLite classify(Schema versionedSchemaParsed, TranslationPlanLite plan) {
          Object payload = versionedSchemaParsed.getParsed()
          BetterJson smart = new BetterJson(payload)
          List messages = smart.asList("msglog", "messages", "message")

          TranslationPlanLite parent = Plans.newParent(
                  Plans.asSchema("batch-msglog", "1.0", "json"),
                  plan.lastSchema())

          for (int i = 0; i< messages.size(); i++) {
              TranslationPlanLite childPlan = Plans.newPlan(
                      Plans.asSchema("${msg-classifier}", "1.0", "json"),
                      Plans.asSchema("std-msg-out", "1.0", "json"))

              Plans.realizeChildPlan(childPlan, "batch-msg-placeholder", versionedSchemaParsed, messages, i)
              parent.addChild(childPlan)
          }

          parent
      }
  }

The most important part of this classifier is the call to realizeChildPlan(). In realizing a plan, the
payload is actually modified (if you printed it, you would see this). In the case of working with translating
each element of the "messages" list independently, the "messages" parent and the index "i" identify which
child is the target of the child translation plan. After this call the modified payload will actually have
a variant of the marker "batch-msg-placeholder" sitting in place of the child portion of the in memory structure.
The child retains a reference to the original element and this will be swapped back in place of the marker
as the parent plan finishes.

Flexible Example
++++++++++++++++

The above logic relies on the classifier author to manually drill into the payload to find the parent
array of interest. In this example, it is possible, but in the general case, say when you have nested arrays,
this can start to get clumsy. To help with this, there is a realizeChildPlans() available (note the plural
name). There are a couple of variants of this, but the example below will show the first version that uses
drilling in to get to the array just like above.

.. code-block:: java

  package acme

  import org.opendaylight.plastic.implementation.Schema
  import org.opendaylight.plastic.implementation.PlanningClassifier
  import org.opendaylight.plastic.implementation.TranslationPlanLite
  import org.opendaylight.plastic.implementation.VersionedSchemaParsed
  import org.opendaylight.plastic.implementation.author.BetterJson
  import org.opendaylight.plastic.implementation.author.Plans

  class BatchMsgClassifier extends PlanningClassifier {

      TranslationPlanLite classify(Schema versionedSchemaParsed, TranslationPlanLite plan) {
          Object payload = versionedSchemaParsed.getParsed()

          List<TranslationPlanLite> children = Plans.realizeChildPlans(
             versionedSchemaParsed,
             payload?.msglog?.messages?.message,
             "batch-msg-placeholder",
             Plans.asSchema("${msg-classifier}", "1.0", "json"),
             Plans.asSchema("std-msg-out", "1.0", "json"))

          TranslationPlanLite parent = Plans.newParent(
                  Plans.asSchema("batch-msglog", "1.0", "json"),
                  plan.lastSchema())

          parent.addChildren(children)
          parent
      }
  }

The above example is slightly simpler than the original version, but it is going to be much
more flexible in complex cases. Note the wise use of the "?." operator; if the payload does
not have that path, then null will be passed into the realizeChildPlans(), which will flag
the problem with an exception.

More Flexibility
++++++++++++++++

The second version of realizeChildPlans() uses a *string path* for the second argument instead
of a direct reference to a part of the payload. This allows a lot of flexibility to indirectly
deal with the iteration of nested arrays.

Lets assume that every message in the payload above now has another array of object inside of
it called "tags". Consider a *string path* like "msglog.messages.message[*].details.tags". If
you use this, then a child plan will be created for each "tag" structure inside of each
"message".

Considerations
++++++++++++++

Here are some important considerations when using child translation plans to segregate your
translation logic:

* You can use any ad hoc iteration you want; the examples above all are based on lists
* You must use unique marker/placeholder names (or it is flagged as an error)
* The payload is sliced up by "realized plans" so order matters for multiple realizeChildPlans() calls
* There may be dependency between parent schema and child schema if parent schema doesn't treat child results opaquely

Chapter 16 - Misc Topics
^^^^^^^^^^^^^^^^^^^^^^^^

Time
++++

A discussion of the standardized time string that is used can be found at
`NETCONF date-and-time
<http://www.netconfcentral.org/modules/ietf-yang-types>`_

The output format can be described by the following regular expression::

\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d)?(Z|[\\-]\d{2}:\d{2})

The supported time input formats (that are convertible to the standard output
format above) are either epoch seconds or a local time that is missing the
time zone (common in Juniper devices).

Epoch seconds is a large integer that represents the number of seconds since
01/01/1970 in GMT.

The local time format is like the ISO 8601 format above, but has a dash
instead of the T and is missing the zone offset. An example would be
2018/06/17-14:36:00.

For routines that need a zone offset, the value can either be "UTC" or
a string with the format [-]dd:dd, where d is a digit and the whole offset
is the hours:minutes off of GMT. An example is "06:00" for US CST.

Note that without a time zone offset, a given time is ambiguous and cannot
be shifted to a known time zone. If an ambigous time is supplied to the
time normalization routines, an ambigous time is returned and can be
recognized by the offset "-00:00" per the NETCONF standard above.

XML vs JSON
+++++++++++

The intent is to keep the features for handling of XML and JSON the same within Plastic.
But right now there are two features that are in JSON but not XML. The first is the use of
the array indexed variables [*] feature. The second is the allowing of non-scalar values for
defaults.

Polling
+++++++

Plastic can periodically poll the file system for changes to classifiers, morphers, and
schemas. This is considered a development feature and normally is disabled. It can be enabled
by supplying an integer polling delay value (in seconds) greater than 0. The value 0 will
disable the polling. Client applications of Plastic usually control this via a property
in a properties file.

Chapter 17 - Best Practices
^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Always favor expressing translations in schemas over morphers, because the former are easier
  to see and the latter tend to obfuscate to a certain degree.

- Always favor tweakInputs() over tweakValues() over tweakParsed() to be as resilient as possible
  with regard to schema changes.

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
  file name (just like Java). You can use an aribtrary directory hierarchy if you avoid
  using packages for your classes (recommended). If you insist on using packages, then the
  directory structure must match the package names.

- Classifiers and morphers should never die - they should gracefully degrade. This means
  they should not abort, throw, or return null. Doing otherwise will make the individual
  translation fail, or if being used in a batching context, the whole batch will fail
  translation.

Appendix - Common Errors and Warnings
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

There are approximately 100 possible errors coming from Plastic. Many of them are very obscure and highly likely
to ever be seen. They really occur during morpher/classifier/schema development, and not during deployment. All of
the errors and warnings are captured in the controller app (karaf) or agent log file. Some of the errors are
exposed as exceptions to callers of the main translate method.

Below is a list of the most commonly occurring Plastic errors and warnings.

Inputs and Outputs
++++++++++++++++++

.. error::

  MissingInputsException ... PLASTIC-MISSING-IN ... For (in-> <input-schema-name>) (out-> <output-schema-name>), the
  following input variables were not found on the incoming payload: [<var-name-1>, <var-name-2>, ...]

.. warning::

  WARN ... For (in-> <input-schema-name>) (out-> <output-schema-name>), the following input variables had no
  matching outputs:  [<var-name-1>, <var-name-2>, ...]

.. error::

  DangingInputsException ... PLASTIC-DANGL-IN ... For (in-> <input-schema-name>) (out-> <output-schema-name>), the
  following input variables were not matched to output variables: [<var-name-1>, <var-name-2>, ...]

.. error::

  DangingOutputsException ... PLASTIC-DANGL-OUT ... For (in-> <input-schema-name>) (out-> <output-schema-name>), the
  following output variables were not matched to input variables: [<var-name-1>, <var-name-2>, ...]

.. error::

  DanglingOutputVariables ... PLASTIC-DANGLING-OUT-VARS ... For (in-> <input-schema-name>) (out-> <output-schema-name>), the
  following output variables had no matching inputs: [<var-name-1>, <var-name-2>, ...]

JSON Payloads
+++++++++++++

All of these errors occur when the morpher or classifier logic processes a JSON payload.

- BetterJsonException ... At the end of the JSON path <dotted.path.in.payload>, found a non-list: <json>
- BetterJsonException ... At the end of the JSON path <dotted.path.in.payload>, found a non-map: <json>
- BetterJsonException ... Cannot determine emptiness of non-collection <json> in morpher <morpher-class-name>
- BetterJsonException ... Could not use JSON path (<dotted.path.in.payload> on a NULL root)
- BetterJsonException ... Using a JSON path (<dotted.path.in.payload>) is not supported for the following: <json>
- BetterJsonException ... Encountered a list index (<integer>) out-of-bounds (length is <integer>) along the path <dotted.path.in.payload>)
- BetterJsonException ... Encountered a missing JSON path component <path.component.name> along the path <dotted.path.in.payload> for object <json>)

Understanding the terminology use in the error messages will help you understand the intent of the
error.

JSON payloads can be thought of as having a hierarchy where the "root" is the top-most element.
A path is a list of JSON key names that are separated by dots and are used as a means of identifying a value
that is deeper in a payload.

Encountering these errors in production usually means that the payload shape (aka schema) was unexpected.
This could be due to a malformed payload but more likely is a payload that was not considered by the designer
of the morpher or classifier.

.. code-block:: json

  {
    "admin": {
      "overall-status": "UP"
    },
    "network-element" : {
        "mtu": "1500",
        "max-delay": "25"
    }
  }

In the above example, valid paths are: admin, admin.overall-status, network-element, network-element.mtu, and
network-element.max-delay. Any individual portion of these paths are called path components, with some examples
being admin, overall-status, and max-delay.

Others
++++++

.. error::

  RuntimeException ... Child plans threw <integer> exceptions - the first one is ... <exception-dump>

Child plans are only used for complex translations, like translating collections of individual items. They
run as part of an overall parent plan. This error means that one or more of the children had a fatal error,
hence the overall translation failed. This is a generic catch of an error and you will have to look at the
child's exception dump to understand more about what the root cause is.

.. error::

  JSONParseException ... Cannot parse XML as JSON

An arriving payload was XML and the translation was set up to accept JSON. This could be the translation
designer's fault, requiring recoding of program logic. But more likely, the device can emit either XML or JSON
and has been mis-configured to emit the wrong payload format.

Appendix
~~~~~~~~
This document can be converted to PDF using `rst2pdf
<https://github.com/rst2pdf/rst2pdf>`_

`RST syntax reference
<http://docutils.sourceforge.net/docs/user/rst/quickref.html>`_
