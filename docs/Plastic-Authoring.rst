.. footer::

  *Copyright (c) 2019 Lumina Networks, Inc. All rights reserved.*
  *This program and the accompanying materials are made available under the*
  *terms of the Eclipse Public License v1.0 which accompanies this distribution,*
  *and is available at http://www.eclipse.org/legal/epl-v10.html*

=================
Plastic Authoring
=================
*Sep 27, 2019*

Introduction
~~~~~~~~~~~~
For a conceptual introduction of how *morphers* and *classifiers* fit into the Plastic
ecosystem, see the companion document Plastic-Introduction

The document is intended for authors of morpher and classifier logic and covers the
different morphers, their callback hooks, and support logic.

Morpher Support
~~~~~~~~~~~~~~~
Almost all morphers should extend from either *BasicMorpher* or from
*ExtendedBasicMorpher*. The former is the older interface and the latter
is the newer one. It is recommended that new morphers extend from the
latter.

The support logic consists of methods and classes that make it more
convenient to write morpher logic. This usually means hiding a lot of
details that would make your morpher methods be longer or more complex.

When your morpher extends from one of those base classes, it will inherit
methods that are part of the parent morpher. There are also helper utility
classes that can be used regardless of which morpher parent class you
extend from.

Many of the inherited methods are shared but some of them have been
moved to other classes in the case of extending from ExtendedBasicMorpher.

If a morpher extends from BasicMorpher, the tweak methods have access to
inherited support methods and data.

Full Morpher Type Signature
~~~~~~~~~~~~~~~~~~~~~~~~~~~

A full type signature for a morpher deriving from *BasicMorpher* looks like this::

  import org.opendaylight.plastic.implementation.BasicMorpher

  class MyBasicMorpher extends BasicMorpher
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

A full type signature for a morpher deriving from *ExtendedBasicMorpher* looks like this::

  import org.opendaylight.plastic.implementation.ExtendedBasicMorpher
  import org.opendaylight.plastic.implementation.author.MoVariables

  class MyExtendedBasicMorpher extends ExtendedBasicMorpher
  {
      void tweakInputs(MoVariables ins, Object payload) {
          ...
      }

      void tweakValues(MoVariables ins, MoVariables outs) {
          ...
      }

      void tweakParsed(Object inTree, Object outTree) {
          ...
      }
  }

The names of the callback hooks is the same, but the arguments are slightly different.
In the former case, the arguments are Maps. In the latter, they are MoVariables, which
is short for "morpher variables" and act as a smart wrapper around a Map.

Common Inherited Methods
~~~~~~~~~~~~~~~~~~~~~~~~
Whether you extend from *BasicMorpher* or *ExtendedBasicMorpher*, there are methods that
you inherit for use in your morpher callback. This section describes those common
methods.

Logging
-------
Common logging routines are available as methods on the classifiers and
morphers. They can use Java "string format" indicators, like %d and %s,
or logging indicators like "{}".

debug("your message here")
  This method will log a debug message into the application log.

info("your message here")
  This method will log an info message into the application log.

warn("your message here")
  This method will log a warning message into the application log.

error("your message here")
  This method will log a warning message into the application log.

abort("your message here")
  This method will halt the translation process with an exception that goes
  back to the originator service, showing the given message as the reason.
  The message will be logged as an error into the application log. You should
  rarely ever need to call this.

Disabling Errors/Warnings
-------------------------

Plastic is very strict about dangling inputs and outputs by design. But
when a morpher is used, it cannot tell exactly how variables are being used,
so its warngings and errors are sometimes unnecessary. Use these routines
to turn off some of these.

ignoreUnusedInputs(...)
  Plastic is strict about making sure that all $ variables in the
  input schema are mapped into the output schema. Morpher writers will often
  write logic to use inputs and this is opaque to Plastic. Call this
  method to tell Plastic which input variables are being taken care of
  by the morpher logic.

  Like the other calls, you can supply one or more variable names. If you use
  () or "*", then it will ignore all unused input errors, so use with care.

ignoreUnusedOutputs(...)
  Plastic is strict about making sure that all $ variables in the
  input schema are mapped into the output schema. Morpher writers will often
  write logic to use outputs and this is opaque to Plastic. Call this
  method to tell Plastic which output variables are being taken care of
  by the morpher logic.

  Like the other calls, you can supply one or more variable names. If you use
  () or "*", then it will ignore all unused output errors, so use with care.

optionalInputs(...)
  Call this from the morpher constructor (or from any other method) to tell
  Plastic that a variable identified in the input schema may or may not
  be found in the incoming payload, and to not halt the mapping with an error
  if that input is not found.

  Pass in one or more quoted variable names. If you call it with nothing (an
  empty parenthesis) or pass in "*", then it means all inputs are optional. Use
  nothing or "*" with care, as all missing variables will no longer be flagged
  as an error.

Others
------

timeFromEpoch(epoch, zoneOffset)
  Call this from your morpher logic to obtain a standardized date/time.
  The epoch is a string version of the epoch seconds and the zone offset
  is the GMT offset. Both are discussed in a section on Time in the
  Plastic-Introduction. The return value is the standardized time.

urlEncode("...")
  This convenience function takes the given input string and does a URL
  encoding of it using the "UTF-8" character set, returning the result.

*BasicMorpher* Inherited Methods
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

ignoreUnused()
  This method is the same as ignoreUnusedInputs() and is deprecated. You
  should use the latter method instead.

isBound("varName")
  Returns true if the given input variable name has a value. All variables
  from a successful mapping have a value unless they are marked as optional
  using optionalInputs().

isEmpty(tree.member)
  Returns true if the given "path" or member is either null (missing) or
  is a collection (list or map) that is empty. If this is called with a
  member that is a scalar (non-collection), then an exception is generated
  resulting in a failed mapping.


*ExtendedBasicMorpher* Inherited Methods
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following methods are not present on *ExtendedBasicMorpher*
* ignoreUnused() was removed, so use ignoreUnusedInputs() instead
* isBound() was moved to the MoVariables class
* isEmpty() was moved to the BetterJson class

BetterJson
~~~~~~~~~~

The BetterJson class is a wrapper class that gives more convenient access to navigating Lists and Maps
in Groovy. These basic types are what JSON is parsed into, hence the name for this class.

BetterJson(Object payload)
  Constructor using an existing payload, which should be a Map or a List. This object is the
  root of the hierarchy.

BetterJson(String raw)
  Constructor using the raw string as JSON, which will be immediately parsed (and any syntax
  exceptions thrown). The parsed object is the root of the hierarchy.

Object asObject(String... path)
  Takes the given path components, access the root, and successively drill down into the root object.
  If any path component cannot be found, throw an exception. Return the target object. If the path
  components are not supplied, then the root is the target object.

List asList(String... path)
  This is very similar to asObject() above, but the target object is returned as a typed List.
  If the target object is not a List, then throw an exception.

Map asMap(String... path)
  This is very similar to asObject() above, but the target object is returned as a typed Map.
  If the target object is not a Map, then throw an exception.

boolean isList(String... path)
  Return true if the path exists and points to a List instance

boolean isNonEmptyList(String... path)
  Return true if the path exists and points to a List instance and that list has 1 or more members

boolean isMap(String... path)
  Return true if the path exists and points to a Map instance

boolean isNonEmptyMap(String... path)
  Return true if the path exists and points to a Map instance and that map has 1 or more keys

Object maybeFetch(String... path)
  Return the Object found at this given path or null if not found

boolean isEmpty(Object tree)
  DEPRECATED - this older method does not fit the philosophy behind this class and should not be used.
  Return true if the tree is either null or if it represents an empty collection (list or map).

Surgeon
~~~~~~~

Like BetterJson, the Surgeon class is a wrapper class that gives convenient means of modifying the
wrapped structure. The structure should be a combination of lists, maps, and scalars - which are
typical results of parsing JSON. It will not work with XML memory model like Node.

Surgeon(Object root)
   Constructor that uses the given root of a map/list/scalar heirarchy

List listify(String... path)
   Walks along the given path and, at the last component, conditionally modifies the wrapped
   structure. If the last component is missing, then an empty list is injected. If a scalar is
   found, then it is wrapped in a list. If a list is found, then no changes are made.

Map mapify(String... path)
   Walks along the given path and, at the last component, conditionally modifies the wrapped
   structure. If the last component is missing, then an empty map is injected. If a scalar is
   found, then it is wrapped in a map using a fixed key of "???". If a map is found, then no
   changes are made.

void placeValue(Object value, String... path)
   Walks along the given path and, at the last component, modifies the wrapped structure by
   injecting the given value. Any existing value is overwritten. The value can be scalar,
   map, or list, or combinations thereof.

MoVariables and MoArray
~~~~~~~~~~~~~~~~~~~~~~~

MoVariables is a wrapper class around a map that allows convenient access to "morpher variables" (its
namesake). An instance of MoVariables is wrapped around the appropriate map and passed into the
morpher callback hooks instead of the maps themselves.

You need the following import statements to use these classes::

    import org.opendaylight.plastic.implementation.author.MoVariables
    import org.opendaylight.plastic.implementation.author.MoArray

MoVariables exposes many of the methods that are already on Map class. This means
that many of the native map syntax and methods are available on MoVariable instances,
including methods like, isEmpty(), size(), containsKey(), etc.

Here are the explicit methods for this class:

MoVariables(Map varBindings)
  Construct a new instance that wraps the given map. Authors should never really need to
  construct a new instance as it is done for you my Plastic.

MoArray asArray(String variable)
  Construct a new instance of an array (discussed later) backed by the same map as this
  instance. The variable name should be an arrayed variable name, like "ADDRESSES[*]"
  that is referenced from your input or output schema. The same backing map will be changed
  when you modify the array.

List<MoArray> asArrays(String... variableNames)
  Similar to the above single slice routine, only this routine creates multiple arrays
  with one call.

boolean contains(String varName)
  Return true if the backing map has an entry keyed by varName.

boolean isBound(String varName)
  Return true of the backing map has an entry keyed by varName and the value
  associated with that key is not null.

void mergeFrom(MoVariables source)
  Take the source and merge it into this instance. This instance will be modified to
  now include keys with values that are not already present. So only keys with values that
  are only in the source will be added to this instance.

MoArray is a wrapper class around a shared map that allows convenient access to arrayed
variables in the underlying map. If you make changes to the array, you are changing the
shared underlying map.

You can get/set an individual member of an array. You can set the whole value for the array (all elements at
once). And there are accessors for size(), isEmpty(), etc. There is not currently a way to incrementally
grow an array by appending onto the end.

Array Creation
--------------
A very common use case is to create an array of data to be expanded into the output schema. The
cononical approach is to just create a normal List, append all the contents, then insert it into
the outputs like this::

    void tweakValues(MoVariables inputs, MoVariables outputs) {
        List abcs = [ ]
        for (...) {
           ...
           abcs.add(value) // append using Java-like syntax, or
           abc << value    // append using Groovy syntax
        }
        outputs.newArray('my-abcs[*]', abcs)
    }

Use Cases (Older Morpher)
~~~~~~~~~~~~~~~~~~~~~~~~~

Conditionally Removing Output Branches
--------------------------------------

This example shows that an input MTU may or may not be on the incoming payload.
The arrayed variables ADDRESS4 and ADDRESS4LEN are marked unused because they
might come in as an empty array. If the MTU input is not present, then we prune
a branch of the output tree (similar for the ip-addresses).

Here is an example morpher using some of the calls above::

  package test

  import org.opendaylight.plastic.implementation.BasicMorpher

  class ConditionalOutputMorpher extends BasicMorpher
  {
      ConditionalOutputMorpher() {
          optionalInputs('MTU')
          ignoreUnusedOutputs('MTU', 'ADDRESS4[*]', 'ADDRESS4LEN[*]')
      }

      void tweakParsed(Object inTree, Object outTree) {
          if (!isBound('MTU')) {
              outTree.requests[0].payload.interface[0].remove('mtu')
          }
          if (isEmpty(inTree.'ip-addresses-v4')) {
              outTree.requests[0].payload.interface[0].remove('vlan-interface-std:vlan')
          }
      }
  }

Using New Support In Older Morphers
-----------------------------------

Here is a use case showing how to use MoVariables in an older morpher::

  import org.opendaylight.plastic.implementation.BasicMorpher

  class MyBasicMorpher extends BasicMorpher
  {
      void tweakValues(Map ins, Map outs) {
          MoVariables ins = new MoVariables(inputs)
          MoVariables outs = new MoVariables(outputs)
          ...
          if (ins.isBound("abc") && outs.isBound("def")) {
              ...
          }
          ...
      }
  }

Use Cases (Newer Morpher)
~~~~~~~~~~~~~~~~~~~~~~~~~

Normalizing Incoming Variables
------------------------------

Here is a use case showing how to modify and validate incoming variable
bindings::

  import org.opendaylight.plastic.implementation.ExtendedBasicMorpher
  import org.opendaylight.plastic.implementation.author.MoVariables

  class MyMorpher extends ExtendedBasicMorpher
  {
      void tweakValues(MoVariables ins, MoVariables outs) {
          outputs.each { k,v -> outputs[k] = v.toUpperCase() }
          if (new Integer(ins['MTU']) < 0) {
              abort("MTU cannot be negative")
          }
          ...
      }
  }

Working With Arrayed Variables
------------------------------

Here is the “masks calculation” use case that shows how to work with arrays::

  import org.opendaylight.plastic.implementation.ExtendedBasicMorpher
  import org.opendaylight.plastic.implementation.author.MoVariables
  import org.opendaylight.plastic.implementation.author.MoArray

  class MyMorpher extends ExtendedBasicMorpher
  {
      void tweakValues(MoVariables ins, MoVariables outs) {
          (MoArray addrs, MoArray prefs) = ins.asArrays(“ip-address[*]”, “prefix-length[*]”)
          MoArray masks = outs.newArray(“MASK[*]”, addrs.size(), "")
          for (int i = 0; i< addrs.size(); i++) {
              masks[i] = myMaskCalculation(addrs[i],prefs[i])
          }
      }
  }

Unioning Inputs-to-Outputs
--------------------------

Here is an example showing the “union inputs to outputs” use case. Note that this is not
declaratively done in the constructor - it is done each time in the tweakValues() body
that you write, but at least it is one line::


  import org.opendaylight.plastic.implementation.ExtendedBasicMorpher
  import org.opendaylight.plastic.implementation.author.MoVariables

  class MyMorpher extends ExtendedBasicMorpher
  {
      void tweakValues(MoVariables ins, MoVariables outs) {
          outs.mergeFrom(ins)
          … more stuff here …
      }
  }

Classifiers
~~~~~~~~~~~

Plastic supports optional classifiers to allow authors to do two different things:
one, to examine incoming payloads to determine which schema to use, and two, to set up
complex translations that allow you to decompose a translation into smaller pieces.

Classifiers have their own directory in the Plastic installation and are snippets of
Groovy. To wire a classifier into a translation, put its Groovy file in the classifiers
directory, then use the classifier name (with variable syntax) instead of the input
schema name. An example should make this clear.

Suppose you have a fixed translation (meaning that you know the input and output schema to
use), so that you use the input schema "abc", "1.0", and "json". If you want to use a
classifier, then your "input schema" might be "${my-input-finder}", "1.0", "json" instead.
Note that we are using the variable syntax of "${...}" wrapped around the classifier name,
which is assumed to be "my-input-finder.groovy".

There are two classifier flavors available and you should choose the one appropriate for
your needs.

Simple Classifier
-----------------

The first is a "simple classifier" that derives from SimpleClassifier. Its purpose is solely
to allow you to look at an incoming payload and to return the base schema name to use for
the translation. An example looks like this::

  import org.opendaylight.plastic.implementation.SimpleClassifier

  class GoodClassifier extends SimpleClassifier {
      String classify(Object parsedPayload) {
          "my-schema-a"
      }
  }

Note that the classify() method is called with a payload to examine. In general, the
payload is either a map (representing parsed JSON) or a node (representing parsed XML).

Planning Classifier
-------------------

There is a more complex classifier available called a PlanningClassifier, which uses a
"translation plan". A translation plan is a list of steps that need to be followed to
do a translation. The simplest plan is just an input schema and an output schema. More
complex plans can be an input schema, one or more morphers, and then an output schema.
Translation plans can also have a parent and child role so that they can be arranged in
a simple heirarchy (multiple levels are *not* supported yet).

So a planning classifier has the full ability to set the input schema, the output
schema, the morphers being used, and to set up child translation plans.

A simple example is::

    import org.opendaylight.plastic.implementation.PlanningClassifier

    class ExampleClassifier extends PlanningClassifier {
        @Override
        TranslationPlanLite classify(Schema payload, TranslationPlanLite plan) {
            Object tree = payload.getParsed()

            ... do something to figure out the input and output schemas ...
            String inschema = ...
            String outschema = ...

            TranslationPlanLite plan = Plans.newPlan(
                    Plans.asSchema(inschema, "1.0", "json"),
                    Plans.asSchema(outschema, "1.0", "json"))

            plan
        }
    }

Planning Classifier Example - Processing A List
-----------------------------------------------

This first example shows how to use a classifier to break down a collection in the payload,
where each element of the collection can be translated in its own right, independent of
everything else. Note that this allows use of a second classifier for the list members!

The example is::

    import org.opendaylight.plastic.implementation.SimpleClassifier

    class ExampleClassifier extends PlanningClassifier {
        @Override
        TranslationPlanLite classify(Schema payload, TranslationPlanLite plan) {
            Object tree = payload.getParsed()
            BetterJson better = new BetterJson(tree)
            List container = better.asList('some', 'path', 'to', 'the', 'collection')

            TranslationPlanLite parent = Plans.newParent(
                    Plans.asSchema("top-level-in-schema", "1.0", "json"),
                    Plans.asSchema("top-level-out-schema", "1.0", "json"))

            for (int i = 0; i< container.size(); i++) {
                TranslationPlanLite child = Plans.newPlan(
                        Plans.asSchema('${low-level-classifier}', "1.0", "json"),
                        Plans.asSchema("low-level-schema", "1.0", "json"))

                Plans.realizeChildPlan(child, "my-example-classifier-marker", payload, container, i)
                parent.addChild(child)
            }

            parent
        }
    }

The above shows a common planning classifier pattern. Set up a parent plan. Loop over the incoming
payload and set up a child plan for each sub-translation. Note that Plastic will partition the
payload into pieces using a placeholder marker and will re-assemble after all the plans execute.

Planning Classifier Example - Processing Multiple Levels
--------------------------------------------------------

This second example is from the CLI-OPS use case::

    package juniper

    import groovy.json.JsonBuilder

    import org.opendaylight.plastic.implementation.Schema
    import org.opendaylight.plastic.implementation.PlanningClassifier
    import org.opendaylight.plastic.implementation.TranslationPlanLite
    import org.opendaylight.plastic.implementation.author.BetterJson
    import org.opendaylight.plastic.implementation.author.Plans

    class OdlJuniperOpsRouteReadResponseClassifier extends PlanningClassifier {

        @Override
        TranslationPlanLite classify(Schema pvs, TranslationPlanLite plan) {
            Object payload = pvs.getParsed()
            surgicallyListify(payload)

            TranslationPlanLite parent = Plans.newParent(
                    Plans.asSchema("batch-odl-juniper-ops-route-read-response-in", "1.0", "json"),
                    Plans.asSchema("batch-odl-juniper-ops-route-read-response-out", "1.0", "json"))

            rtEntries(payload) { uberindex, p, i ->
                TranslationPlanLite child = Plans.newPlan(
                        Plans.asSchema("odl-juniper-ops-route-table-entry-in", "1.0", "json"),
                        Plans.asSchema("odl-juniper-ops-route-table-entry-out", "1.0", "json"))

                Plans.realizeChildPlan(child, "odl-juniper-ops-rte-marker-${uberindex}", pvs, p, i)
                parent.addChild(child)
            }

            routeTables(payload) { p, i ->
                TranslationPlanLite child = Plans.newPlan(
                        Plans.asSchema("odl-juniper-ops-route-table-in", "1.0", "json"),
                        Plans.asSchema("odl-juniper-ops-route-table-out", "1.0", "json"))

                Plans.realizeChildPlan(child, "odl-juniper-ops-rt-marker", pvs, p, i)
                parent.addChild(child)
            }

            parent
        }

        void surgicallyListify(Object payload) {
            BetterJson smart = new BetterJson(payload)
            List rtables = smart.asList('output0', 'output', 'route-information', 'route-table')

            int injectedRts = 0
            int wrappedRts = 0
            int normalRts = 0

            int wrappedRtes = 0
            int injectedNhs = 0
            int injectedNhKeys = 0

            for (int i = 0; i < rtables.size(); i++) {
                Object table = rtables[i]
                Object rt = table['rt']
                if (rt == null) {
                    table['rt'] = []
                    rt = table['rt']
                    injectedRts++
                }
                else if (!(rt instanceof List)) {
                    table['rt'] = [ rt ]
                    wrappedRts++
                }
                else {
                    normalRts++
                }

                List rts = table['rt']
                for (int j = 0; j< rts.size(); j++) {
                    Object par = rts[j]
                    Object rte = par['rt-entry']
                    if (!(rte && rte instanceof List)) {
                        par['rt-entry'] = [ rte ]
                        rte = par['rt-entry']
                        wrappedRtes++
                    }

                    for (int k = 0; k< rte.size(); k++) {
                        Object o = rte[k]
                        if(!o.containsKey('nh')) {
                            o['nh'] = [:]
                            injectedNhs++
                        }
                        o = o['nh']
                    }
                }
            }

            /*
            println("")
            println("${rtables.size()} - found 'route-table'")
            println("${injectedRts} - injected [] for missing 'rt' table")
            println("${wrappedRts} - wrapped list around 'rt' singleton")
            println("${normalRts} - normal 'rt'")
            println("${wrappedRtes} - wrapped list around 'rt-entry' singleton")
            println("${injectedNhs} - injected missing 'nh'")
            println("${injectedNhKeys} - injected missing 'nh' keys of 'to', 'via', 'selected-next-hop', or 'nh-local-interface'")
            println("")
            */
        }

        void rtEntries (Object payload, Closure cl) {
            BetterJson smart = new BetterJson(payload)
            List rtables = smart.asList('output0', 'output', 'route-information', 'route-table')

            for (int i = 0; i < rtables.size(); i++) {
                Object table = rtables[i]
                List rts = table['rt']
                for (int j = 0; j< rts.size(); j++) {
                    cl(i, rts, j)
                }
            }
        }

        void routeTables (Object payload, Closure cl) {
            BetterJson smart = new BetterJson(payload)
            List rtables = smart.asList('output0', 'output', 'route-information', 'route-table')

            for (int i = 0; i < rtables.size(); i++) {
                cl(rtables, i)
            }
        }
    }
