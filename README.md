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

```
mvn clean install
```
    
Running with the current master
----------

1. Now try running the system using the example data:
    1. First, get the models and resources:
        1. English: http://accra.sp.cs.cmu.edu/~zhengzhl/event_models/event_english_run_resources.tar.gz
        1. Chinese: http://accra.sp.cs.cmu.edu/~zhengzhl/event_models/event_chinese_run_resources.tar.gz
        1. Put it a directory, we represent as <models_and_resources>
    1. Locate the setting file: 
        1. English setting: settings/nugget/event-run.en.properties
        1. Chinese setting: settings/nugget/event-run.zh.properties
    1. Modify the settings:
        1. edu.cmu.cs.lti.model.dir=<models_and_resources>
        1. edu.cmu.cs.lti.resource.dir=<models_and_resources>
        1. For English: edu.cmu.cs.lti.model.event.dir=<models_and_resources>/EventMention/english
        1. For Chinese: edu.cmu.cs.lti.model.event.dir=<models_and_resources>/EventMention/chinese
    1. Run English:
        1. Just Run it:
            ```
            bin/kbp/run_only_pipeline.sh settings/nugget/event-run.en.properties data/samples/en data/samples/en/output experiment_en_01
            ```           
    1. Run Chinese:
        1. Prerequisites:
            1. Add the LTP JNI to environment variable:
                ```
                export LD_LIBRARY_PATH=<models_and_resources>/ltp/lib:$LD_LIBRARY_PATH
                ```               
            1. Run it:                          
                ```
                bin/kbp/run_only_pipeline.sh settings/nugget/event-run.zh.properties data/samples/zh data/samples/zh/output experiment_zh_01
                ```
    1. The results can be found in two formats:
        1. Json format in: data/samples/en/output/rich/test_run
        1. TBF format in: data/samples/en/output/experiments/test_run/results/all/vanillaMention.tbf
    1. Notes:
        1. The last two parameters of the shell script specify the output directory and the experiment name.
        1. The Chinese system relies on some external tools that requires C++ binaries, which may cause problems on some platforms.
  

Running with an old model (20160411)
----------
1. Download a copy of all the models package, and unpack it: 
   > http://cairo.lti.cs.cmu.edu/~hector/models/EventMentionModelsAndResources20160411.tar.gz
1. Currently the project has been refactored a lot:
    1. To run these old models, try an earlier branch:
    1. https://bitbucket.org/hunterhector/cmu-script/branch/model0411
1. Modify the kbp.properties file with the following:
    1. point edu.cmu.cs.lti.model.dir to the unpacked directory
    2. point edu.cmu.cs.lti.model.event.dir to the unpacked directory
    3. point edu.cmu.cs.lti.resource.dir to the unpacked directory
1. Test it out by running the following command in cmu-script project directory:

        bin/test/coref_plain_text.sh settings/kbp.properties event-coref/src/test/resources/sample-input ../sample-output
    
1. You should be able to find the annotation in TBF format in the following file:
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