# odl-plastic

This is a "translation by intent" facility that provides leverage in the model-to-model transform space. 
The model-to-model translation problem is pervasive in writing SDN controller applications, both internally 
and in supporting micro services. By convention, Plastic emphasizes writing translations that would be as 
resilient to model changes as possible. This is implemented through various mechanisms from declarative 
expressions of schemas, through schema semi-independent morphing, up to unbounded data morphing.

## Getting Started

To begin, ensure that you have the prerequisite software installed as enumerated below. You will 
need internet access to pull down the project dependencies from a public Maven repository. 

Use git to clone or fork the repository.

### Prerequisites

To build this repository, you will need the following installed on your machine:

* git 2.14+
* Java 8 JDK
* Maven 3.0+
* rst2pdf (if you want to generate documentation)

### Installing

Once you have the prerequisites and have cloned the repo, you can issue a build at the 
top level of your local copy of the repo

```
mvn clean install
```

The build should complete normally. You can look in the target directory for artifacts.
There should be a plastic-*.jar and a directory called runner. If you change your current
working directory to the runner directory, you can issue the following command to see
things work (this uses examples from the tutorial)
 
```
./runner runnerroot.properties
```

You should see log output that shows a successful translation from "abcd" to "ABCD". 

## Running the tests

### Unit tests

Most of the testing is done using unit tests that are written using Spock (a highly recommended
alternative to JUnit). These tests are run as part of every single build and a failure of
a unit test breaks the build.

### Manual tutorial examples

There is a set of tutorials in the target/runner directory. You can find them as *.RST files. You 
can install rst2pdf and convert them to PDF if you'd like.

From the target/runner directory, you can execute any of the tutorial examples
using a command like

```
./runner <name>.properties
```

## Built With

* [Git](https://git-scm.com/) - Repository Management
* [Maven](https://maven.apache.org/) - Dependency Management

## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/your/project/tags).

## Authors

* **Allan Clarke** - *Conceptualizer, lead, initial work*
* **Mike Arsenault** - *Initial work*
* **Balaji Varadaraju** - *Conceptualization and requirements*

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

This project is licensed under the EPL1 License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

* Lumina Networks, Inc, for allowing this code to be open-sourced
* Balaji Varadaraju, for discussing ideas and requirements wrangling
* Mike Arsenault, for discussing ideas and contributing
* Shaleen Saxeena, for pushing the boundaries around requirements

