# README #

The event project
-----------------

 - Quick summary

A research prototype that induce script knowledge,mining relations from general discourse components (events, entities) from text, from multiple documents (Or not).

Requirements
----------
The project is developed under the following environment:

1. Java 1.8  (A Java 1.6 branch is available, but not frequently maintained).
2. Maven 3.2.1

In addition, it depends on the following modules,

1. Uima Tools : https://bitbucket.org/hunterhector/uima-base-tools
2. Other Utilities : https://bitbucket.org/hunterhector/zl-utils

Building is simple with Maven, under the root directory of the project, do (You need to do the same thing for the two modules I mentioned above beforehand.):
    
    mvn clean install
    
Running with existing model
----------
1. Download a copy of all the models package, and unpack it: 
    > http://cairo.lti.cs.cmu.edu/~hector/models/EventMentionModelsAndResources20160411.tar.gz
2. Modify the kbp.properties file with the following:
    1. point edu.cmu.cs.lti.model.dir to the unpacked directory
    2. point edu.cmu.cs.lti.model.event.dir to the unpacked directory
    3. point edu.cmu.cs.lti.resource.dir to the unpacked directory
3. Test it out by running the following command in cmu-script project directory:

        bin/test/coref_plain_text.sh settings/kbp.properties event-coref/src/test/resources/sample-input ../sample-output
    
4. You should be able to find the annotation in TBF format in the following file:
   > ../sample-output/eval/full_run/lv1_coref.tbf

For details about the TBF format, scoring, visit the TAC KBP event task website (look for Task Definition):
   > http://cairo.lti.cs.cmu.edu/kbp/2015/event
   
**Notes:**
    The current models are not the best models since the project is subject to frequent changes recently, I will try to update it as soon as possible.

**Warning about Illegal Thread Exception**
If you see a java.lang.IllegalThreadStateException saying some threads are not terminated, but also a "BUILD SUCCESS" message, that should be fine. I don't know the reasons for it right now.

About The Configuration File
----------
1. The kbp.properties file contains most of the configuration, most of them are pointers to resources. Some numbers controls the various parameters for training.
2. Most boolean configuration with a "skip" in it will try to skip certain step if the specific output exists, be ware to turn it off when you want to have fresh results.
3. Detailed explanation of the parameters will come later.


Training the model
----------
To train the model, the easiest way is to use the data provided by [TAC-KBP 2015](http://www.nist.gov/tac/2015/KBP/data.html). One can also create files of similar format to train them.