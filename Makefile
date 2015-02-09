

#########################################

#SET THE DIRECTORY
export logDir=log
export LIMODIR=/misc/proteus108/thien/projects/limo-re

SVM_DIR=$(LIMODIR)/svm_semantic
#####

DATADIR=data/ace2005

WACKYMATRIX=$(LIMODIR)/matrix/lsa_ukwac_all_ace2005vocabOnly.txt/matrix.bin
W2VMATRIX=$(LIMODIR)/matrix/word2vec/word2vecACE.sim.bin
TURIANMATRIX=$(LIMODIR)/matrix/word2vec/turian4ace.sim.bin
EMPTYMATRIX=$(LIMODIR)/matrix/empty/matrix.bin


D=true
GR=type
# to use combined kernel between trees and vectors
C=L
# the parameter for traffoff between tree and vector kernels
Tc=0.7
# the type of vector kernel (see function basic_kernel in kernel_tree.c for details, consider to use sigmoid ...
S=1

## configuration files
CHARNIAK_ZHANG=conf-train-test-charniak-zhang-ry.xml
CHARNIAK_ZHANG_WC=conf-train-test-charniak-zhang-ry-wc.xml
CHARNIAK_ZHANG_SEMTK=conf-train-test-charniak-zhang-ry-semkernel.xml
CHARNIAK_ZHANG_W2VSEMTK=conf-train-test-charniak-zhang-ry-word2vecsemkernel.xml
CHARNIAK_ZHANG_WC_W2VSEMTK=conf-train-test-charniak-zhang-ry-word2vecsemkernel-wc.xml
CHARNIAK_ZHANG_WC_SEMTK=conf-train-test-charniak-zhang-ry-semkernel-wc.xml

CHARNIAK_ZHANG_TURIANSEMTK=conf-train-test-charniak-zhang-ry-turiansemkernel.xml

CHARNIAK_ZHANG_VEC=conf-train-test-charniak-zhang-ry-vec.xml
CHARNIAK_ZHANG_WC_VEC=conf-train-test-charniak-zhang-ry-wc-vec.xml
CHARNIAK_ZHANG_SEMTK_VEC=conf-train-test-charniak-zhang-ry-semkernel-vec.xml
CHARNIAK_ZHANG_WC_SEMTK_VEC=conf-train-test-charniak-zhang-ry-semkernel-wc-vec.xml

CHARNIAK_ZHANG_EXAMPLE=conf-train-test-charniak-zhang-ry-example.xml

#########################################


example:
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG_EXAMPLE) TRAINDIR=nw_bn_example-RY TESTDIR=bc_example-RY  > $(logDir)/nohup.example-baseline-charniak.$(D).f1.PET.c2.4 



### baselines                                                                                                                                                                                                                                                                     
baseline-pet0:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f0.PET.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train0-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f0.PET.c2.4 
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train0-RY TESTDIR=cts-RY > $(logDir)/nohup.cts-baseline-charniak.$(D).f0.PET.c2.4 
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train0-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f0.PET.c2.4

baseline-pet1:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f1.PET.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train1-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f1.PET.c2.4 
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train1-RY TESTDIR=cts-RY > $(logDir)/nohup.cts-baseline-charniak.$(D).f1.PET.c2.4 
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train1-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f1.PET.c2.4

baseline-pet2:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f2.PET.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train2-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f2.PET.c2.4 
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train2-RY TESTDIR=cts-RY > $(logDir)/nohup.cts-baseline-charniak.$(D).f2.PET.c2.4 
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train2-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f2.PET.c2.4
	
baseline-pet3:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f3.PET.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train3-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f3.PET.c2.4 
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train3-RY TESTDIR=cts-RY > $(logDir)/nohup.cts-baseline-charniak.$(D).f3.PET.c2.4 
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train3-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f3.PET.c2.4
	
baseline-pet4:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f4.PET.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train4-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f4.PET.c2.4 
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train4-RY TESTDIR=cts-RY > $(logDir)/nohup.cts-baseline-charniak.$(D).f4.PET.c2.4 
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train4-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f4.PET.c2.4
	
baseline-pet-bc:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f.PET.c2.4
	
baseline-pet-bc0:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn-RY TESTDIR=bc0-RY  > $(logDir)/nohup.bc0-baseline-charniak.$(D).f.PET.c2.4

baseline-pet-cts:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn-RY TESTDIR=cts-RY > $(logDir)/nohup.cts-baseline-charniak.$(D).f.PET.c2.4
	
baseline-pet-wl:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=PET c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f.PET.c2.4

baseline-bow0:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f0.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train0-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f0.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train0-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-baseline-charniak.$(D).f0.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train0-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f0.BOW.c2.4
	
baseline-bowM0:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f0.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train0-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f0.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train0-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-baseline-charniak.$(D).f0.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train0-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f0.BOW_M.c2.4
	
baseline-bow1:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f1.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train1-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f1.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train1-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-baseline-charniak.$(D).f1.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train1-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f1.BOW.c2.4
	
baseline-bowM1:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f1.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train1-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f1.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train1-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-baseline-charniak.$(D).f1.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train1-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f1.BOW_M.c2.4
	
baseline-bow2:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f2.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train2-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f2.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train2-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-baseline-charniak.$(D).f2.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train2-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f2.BOW.c2.4
	
baseline-bowM2:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f2.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train2-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f2.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train2-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-baseline-charniak.$(D).f2.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train2-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f2.BOW_M.c2.4
	
baseline-bow3:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f3.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train3-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f3.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train3-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-baseline-charniak.$(D).f3.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train3-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f3.BOW.c2.4
	
baseline-bowM3:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f3.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train3-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f3.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train3-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-baseline-charniak.$(D).f3.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train3-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f3.BOW_M.c2.4
	
baseline-bow4:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f4.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train4-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f4.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train4-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-baseline-charniak.$(D).f4.BOW.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train4-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f4.BOW.c2.4
	
baseline-bowM4:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f4.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train4-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f4.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train4-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-baseline-charniak.$(D).f4.BOW_M.c2.4
	#nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn_train4-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f4.BOW_M.c2.4

baseline-bow-bc:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f.BOW.c2.4

baseline-bowM-bc:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f.BOW_M.c2.4
	
baseline-bow-cts:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-baseline-charniak.$(D).f.BOW.c2.4

baseline-bowM-cts:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-baseline-charniak.$(D).f.BOW_M.c2.4
	
baseline-bow-wl:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f.BOW.c2.4

baseline-bowM-wl:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk F=1 TREETYPE=BOW_M c=2.4 CONF=$(CHARNIAK_ZHANG) TRAINDIR=nw_bn-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f.BOW_M.c2.4

### before running this, adjust the paths in the CHARNIAK_ZHANG_WC_SEMTK file!
system-PETmpwc0:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk T=5 F=1 TREETYPE=PETmpwc c=2.4 CONF=$(CHARNIAK_ZHANG_WC) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY > $(logDir)/nohup.indomain-charniak.$(D).f0.PET.F1.c2.4.PETmpwc
	
system-PETmpwc1:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk T=5 F=1 TREETYPE=PETmpwc c=2.4 CONF=$(CHARNIAK_ZHANG_WC) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY > $(logDir)/nohup.indomain-charniak.$(D).f1.PET.F1.c2.4.PETmpwc

system-PETmpwc2:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk T=5 F=1 TREETYPE=PETmpwc c=2.4 CONF=$(CHARNIAK_ZHANG_WC) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY > $(logDir)/nohup.indomain-charniak.$(D).f2.PET.F1.c2.4.PETmpwc

system-PETmpwc3:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk T=5 F=1 TREETYPE=PETmpwc c=2.4 CONF=$(CHARNIAK_ZHANG_WC) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY > $(logDir)/nohup.indomain-charniak.$(D).f3.PET.F1.c2.4.PETmpwc

system-PETmpwc4:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk T=5 F=1 TREETYPE=PETmpwc c=2.4 CONF=$(CHARNIAK_ZHANG_WC) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY > $(logDir)/nohup.indomain-charniak.$(D).f4.PET.F1.c2.4.PETmpwc

system-PETmpwc-bc:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk T=5 F=1 TREETYPE=PETmpwc c=2.4 CONF=$(CHARNIAK_ZHANG_WC) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY > $(logDir)/nohup.bc1-charniak.$(D).f.PET.F1.c2.4.PETmpwc
	
system-PETmpwc-cts:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk T=5 F=1 TREETYPE=PETmpwc c=2.4 CONF=$(CHARNIAK_ZHANG_WC) TRAINDIR=nw_bn-RY TESTDIR=cts-RY > $(logDir)/nohup.cts-charniak.$(D).f.PET.F1.c2.4.PETmpwc
	
system-PETmpwc-wl:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk T=5 F=1 TREETYPE=PETmpwc c=2.4 CONF=$(CHARNIAK_ZHANG_WC) TRAINDIR=nw_bn-RY TESTDIR=wl-RY > $(logDir)/nohup.wl-charniak.$(D).f.PET.F1.c2.4.PETmpwc

####

system-PETlsa0:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f0.PET.F1.c2.4.PETlsa
	
system-PETlsa1:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f1.PET.F1.c2.4.PETlsa
	
system-PETlsa2:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f2.PET.F1.c2.4.PETlsa
	
system-PETlsa3:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f3.PET.F1.c2.4.PETlsa
	
system-PETlsa4:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f4.PET.F1.c2.4.PETlsa
	
system-PETlsa-bc:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.bc1-charniak.$(D).f.PET.F1.c2.4.PETlsa
	
system-PETlsa-cts:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK) TRAINDIR=nw_bn-RY TESTDIR=cts-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.cts-charniak.$(D).f.PET.F1.c2.4.PETlsa
	
system-PETlsa-wl:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK) TRAINDIR=nw_bn-RY TESTDIR=wl-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.wl-charniak.$(D).f.PET.F1.c2.4.PETlsa


####

system-PETw2v0:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETw2v c=2.4 CONF=$(CHARNIAK_ZHANG_W2VSEMTK) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY MATRIX=$(W2VMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f0.PET.F1.c2.4.PETw2v
	
system-PETw2v1:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETw2v c=2.4 CONF=$(CHARNIAK_ZHANG_W2VSEMTK) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY MATRIX=$(W2VMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f1.PET.F1.c2.4.PETw2v
	
system-PETw2v2:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETw2v c=2.4 CONF=$(CHARNIAK_ZHANG_W2VSEMTK) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY MATRIX=$(W2VMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f2.PET.F1.c2.4.PETw2v
	
system-PETw2v3:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETw2v c=2.4 CONF=$(CHARNIAK_ZHANG_W2VSEMTK) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY MATRIX=$(W2VMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f3.PET.F1.c2.4.PETw2v
	
system-PETw2v4:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETw2v c=2.4 CONF=$(CHARNIAK_ZHANG_W2VSEMTK) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY MATRIX=$(W2VMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f4.PET.F1.c2.4.PETw2v
	
system-PETw2v-bc:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETw2v c=2.4 CONF=$(CHARNIAK_ZHANG_W2VSEMTK) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY MATRIX=$(W2VMATRIX) > $(logDir)/nohup.bc1-charniak.$(D).f.PET.F1.c2.4.PETw2v
	
system-PETw2v-cts:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETw2v c=2.4 CONF=$(CHARNIAK_ZHANG_W2VSEMTK) TRAINDIR=nw_bn-RY TESTDIR=cts-RY MATRIX=$(W2VMATRIX) > $(logDir)/nohup.cts-charniak.$(D).f.PET.F1.c2.4.PETw2v
	
system-PETw2v-wl:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETw2v c=2.4 CONF=$(CHARNIAK_ZHANG_W2VSEMTK) TRAINDIR=nw_bn-RY TESTDIR=wl-RY MATRIX=$(W2VMATRIX) > $(logDir)/nohup.wl-charniak.$(D).f.PET.F1.c2.4.PETw2v

####

system-PET+PETwc+PETlsa0:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f0.PET.F1.c2.4.PET+PETwc+PETlsa
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train0-RY TESTDIR=bc1-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.bc1-charniak.$(D).f0.PET.F1.c2.4.PET+PETwc+PETlsa 
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train0-RY TESTDIR=cts-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.cts-charniak.$(D).f0.PET.F1.c2.4.PET+PETwc+PETlsa 
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train0-RY TESTDIR=wl-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.wl-charniak.$(D).f0.PET.F1.c2.4.PET+PETwc+PETlsa 

system-PET+PETwc+PETlsa1:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f1.PET.F1.c2.4.PET+PETwc+PETlsa
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train1-RY TESTDIR=bc1-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.bc1-charniak.$(D).f1.PET.F1.c2.4.PET+PETwc+PETlsa 
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train1-RY TESTDIR=cts-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.cts-charniak.$(D).f1.PET.F1.c2.4.PET+PETwc+PETlsa 
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train1-RY TESTDIR=wl-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.wl-charniak.$(D).f1.PET.F1.c2.4.PET+PETwc+PETlsa 
	
system-PET+PETwc+PETlsa2:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f2.PET.F1.c2.4.PET+PETwc+PETlsa
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train2-RY TESTDIR=bc1-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.bc1-charniak.$(D).f2.PET.F1.c2.4.PET+PETwc+PETlsa 
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train2-RY TESTDIR=cts-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.cts-charniak.$(D).f2.PET.F1.c2.4.PET+PETwc+PETlsa 
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train2-RY TESTDIR=wl-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.wl-charniak.$(D).f2.PET.F1.c2.4.PET+PETwc+PETlsa 
	
system-PET+PETwc+PETlsa3:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f3.PET.F1.c2.4.PET+PETwc+PETlsa
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train3-RY TESTDIR=bc1-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.bc1-charniak.$(D).f3.PET.F1.c2.4.PET+PETwc+PETlsa 
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train3-RY TESTDIR=cts-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.cts-charniak.$(D).f3.PET.F1.c2.4.PET+PETwc+PETlsa 
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train3-RY TESTDIR=wl-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.wl-charniak.$(D).f3.PET.F1.c2.4.PET+PETwc+PETlsa 
	
system-PET+PETwc+PETlsa4:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.indomain-charniak.$(D).f4.PET.F1.c2.4.PET+PETwc+PETlsa
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train4-RY TESTDIR=bc1-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.bc1-charniak.$(D).f4.PET.F1.c2.4.PET+PETwc+PETlsa 
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train4-RY TESTDIR=cts-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.cts-charniak.$(D).f4.PET.F1.c2.4.PET+PETwc+PETlsa 
	#nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn_train4-RY TESTDIR=wl-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.wl-charniak.$(D).f4.PET.F1.c2.4.PET+PETwc+PETlsa 

system-PET+PETwc+PETlsa-bc:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.bc1-charniak.$(D).f.PET.F1.c2.4.PET+PETwc+PETlsa
	
system-PET+PETwc+PETlsa-cts:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn-RY TESTDIR=cts-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.cts-charniak.$(D).f.PET.F1.c2.4.PET+PETwc+PETlsa
	
system-PET+PETwc+PETlsa-wl:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK) TRAINDIR=nw_bn-RY TESTDIR=wl-RY MATRIX=$(WACKYMATRIX) > $(logDir)/nohup.wl-charniak.$(D).f.PET.F1.c2.4.PET+PETwc+PETlsa

#### for embedding features                                                                                                                                                                                                                                                       
system-PET+VEChm0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f0.PET+VEChm.c2.4

system-PET+VEChm1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f1.PET+VEChm.c2.4

system-PET+VEChm2:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f2.PET+VEChm.c2.4
	
system-PET+VEChm3:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f3.PET+VEChm.c2.4
	
system-PET+VEChm4:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY  > $(logDir)/nohup.indomain-baseline-charniak.$(D).f4.PET+VEChm.c2.4
	
system-PET+VEChm-bc:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-baseline-charniak.$(D).f.PET+VEChm.c2.4

system-PET+VEChm-cts:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=cts-RY > $(logDir)/nohup.cts-baseline-charniak.$(D).f.PET+VEChm.c2.4
	
system-PET+VEChm-wl:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-baseline-charniak.$(D).f.PET+VEChm.c2.4
	
####### on dev set

system-PETtur-bc0:
	mkdir -p $(logDir)
	nohup make t5-tree-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETtur c=2.4 CONF=$(CHARNIAK_ZHANG_TURIANSEMTK) TRAINDIR=nw_bn-RY TESTDIR=bc0-RY MATRIX=$(TURIANMATRIX) > $(logDir)/nohup.bc0-charniak.$(D).f.PETtur.c2.4

system-PET+VEChm-bc0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc0-RY  > $(logDir)/nohup.bc0-vec-charniak.$(D).f.PET+VEChm.c2.4
	
system-PET+VECphrase-bc0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc0-RY  > $(logDir)/nohup.bc0-vec-charniak.$(D).f.PET+VECphrase.c2.4
	
system-PET+VECtree-bc0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VECtree c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc0-RY  > $(logDir)/nohup.bc0-vec-charniak.$(D).f.PET+VECtree.c2.4
	
system-PET+VEChm+VECphrase-bc0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc0-RY  > $(logDir)/nohup.bc0-vec-charniak.$(D).f.PET+VEChm+VECphrase.c2.4

system-PET+VEChm+VECtree-bc0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm+VECtree c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc0-RY  > $(logDir)/nohup.bc0-vec-charniak.$(D).f.PET+VEChm+VECtree.c2.4
	
system-PET+VECphrase+VECtree-bc0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VECphrase+VECtree c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc0-RY  > $(logDir)/nohup.bc0-vec-charniak.$(D).f.PET+VECphrase+VECtree.c2.4
	
system-PET+VEChm+VECphrase+VECtree-bc0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm+VECphrase+VECtree c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc0-RY  > $(logDir)/nohup.bc0-vec-charniak.$(D).f.PET+VEChm+VECphrase+VECtree.c2.4

# => VEChm+VECphrase is the best

####### real expriment with VEChm+VECphrase

##PET

system-PET+VEChm+VECphrase0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f0.PET+VEChm+VECphrase.c2.4
	
system-PET+VEChm+VECphrase1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f1.PET+VEChm+VECphrase.c2.4

system-PET+VEChm+VECphrase2:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f2.PET+VEChm+VECphrase.c2.4
	
system-PET+VEChm+VECphrase3:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f3.PET+VEChm+VECphrase.c2.4
	
system-PET+VEChm+VECphrase4:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f4.PET+VEChm+VECphrase.c2.4

system-PET+VEChm+VECphrase-bc1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-vec-charniak.$(D).f.PET+VEChm+VECphrase.c2.4
	
system-PET+VEChm+VECphrase-cts:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-vec-charniak.$(D).f.PET+VEChm+VECphrase.c2.4
	
system-PET+VEChm+VECphrase-wl:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_VEC) TRAINDIR=nw_bn-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-vec-charniak.$(D).f.PET+VEChm+VECphrase.c2.4

##PETwc
system-PETwc+VEChm+VECphrase0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f0.PETwc+VEChm+VECphrase.c2.4
	
system-PETwc+VEChm+VECphrase1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f1.PETwc+VEChm+VECphrase.c2.4

system-PETwc+VEChm+VECphrase2:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f2.PETwc+VEChm+VECphrase.c2.4
	
system-PETwc+VEChm+VECphrase3:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f3.PETwc+VEChm+VECphrase.c2.4
	
system-PETwc+VEChm+VECphrase4:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f4.PETwc+VEChm+VECphrase.c2.4

system-PETwc+VEChm+VECphrase-bc1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-vec-charniak.$(D).f.PETwc+VEChm+VECphrase.c2.4
	
system-PETwc+VEChm+VECphrase-cts:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-vec-charniak.$(D).f.PETwc+VEChm+VECphrase.c2.4
	
system-PETwc+VEChm+VECphrase-wl:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-vec-charniak.$(D).f.PETwc+VEChm+VECphrase.c2.4
	
##PETlsa
system-PETlsa+VEChm+VECphrase0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f0.PETlsa+VEChm+VECphrase.c2.4
	
system-PETlsa+VEChm+VECphrase1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f1.PETlsa+VEChm+VECphrase.c2.4

system-PETlsa+VEChm+VECphrase2:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f2.PETlsa+VEChm+VECphrase.c2.4
	
system-PETlsa+VEChm+VECphrase3:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f3.PETlsa+VEChm+VECphrase.c2.4
	
system-PETlsa+VEChm+VECphrase4:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f4.PETlsa+VEChm+VECphrase.c2.4

system-PETlsa+VEChm+VECphrase-bc1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.bc1-vec-charniak.$(D).f.PETlsa+VEChm+VECphrase.c2.4
	
system-PETlsa+VEChm+VECphrase-cts:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn-RY TESTDIR=cts-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.cts-vec-charniak.$(D).f.PETlsa+VEChm+VECphrase.c2.4
	
system-PETlsa+VEChm+VECphrase-wl:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn-RY TESTDIR=wl-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.wl-vec-charniak.$(D).f.PETlsa+VEChm+VECphrase.c2.4

##PET+PETwc
system-PET+PETwc+VEChm+VECphrase0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f0.PET+PETwc+VEChm+VECphrase.c2.4
	
system-PET+PETwc+VEChm+VECphrase1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f1.PET+PETwc+VEChm+VECphrase.c2.4

system-PET+PETwc+VEChm+VECphrase2:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f2.PET+PETwc+VEChm+VECphrase.c2.4
	
system-PET+PETwc+VEChm+VECphrase3:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f3.PET+PETwc+VEChm+VECphrase.c2.4
	
system-PET+PETwc+VEChm+VECphrase4:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY  > $(logDir)/nohup.indomain-vec-charniak.$(D).f4.PET+PETwc+VEChm+VECphrase.c2.4

system-PET+PETwc+VEChm+VECphrase-bc1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY  > $(logDir)/nohup.bc1-vec-charniak.$(D).f.PET+PETwc+VEChm+VECphrase.c2.4
	
system-PET+PETwc+VEChm+VECphrase-cts:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn-RY TESTDIR=cts-RY  > $(logDir)/nohup.cts-vec-charniak.$(D).f.PET+PETwc+VEChm+VECphrase.c2.4
	
system-PET+PETwc+VEChm+VECphrase-wl:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk F=1 TREETYPE=PET+PETwc+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_VEC) TRAINDIR=nw_bn-RY TESTDIR=wl-RY  > $(logDir)/nohup.wl-vec-charniak.$(D).f.PET+PETwc+VEChm+VECphrase.c2.4

##PET+PETlsa
system-PET+PETlsa+VEChm+VECphrase0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f0.PET+PETlsa+VEChm+VECphrase.c2.4
	
system-PET+PETlsa+VEChm+VECphrase1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f1.PET+PETlsa+VEChm+VECphrase.c2.4

system-PET+PETlsa+VEChm+VECphrase2:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f2.PET+PETlsa+VEChm+VECphrase.c2.4
	
system-PET+PETlsa+VEChm+VECphrase3:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f3.PET+PETlsa+VEChm+VECphrase.c2.4
	
system-PET+PETlsa+VEChm+VECphrase4:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f4.PET+PETlsa+VEChm+VECphrase.c2.4

system-PET+PETlsa+VEChm+VECphrase-bc1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.bc1-vec-charniak.$(D).f.PET+PETlsa+VEChm+VECphrase.c2.4
	
system-PET+PETlsa+VEChm+VECphrase-cts:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn-RY TESTDIR=cts-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.cts-vec-charniak.$(D).f.PET+PETlsa+VEChm+VECphrase.c2.4
	
system-PET+PETlsa+VEChm+VECphrase-wl:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_SEMTK_VEC) TRAINDIR=nw_bn-RY TESTDIR=wl-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.wl-vec-charniak.$(D).f.PET+PETlsa+VEChm+VECphrase.c2.4

##PET+PETwc+PETlsa
system-PET+PETwc+PETlsa+VEChm+VECphrase0:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK_VEC) TRAINDIR=nw_bn_train0-RY TESTDIR=nw_bn_test0-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f0.PET+PETwc+PETlsa+VEChm+VECphrase.c2.4
	
system-PET+PETwc+PETlsa+VEChm+VECphrase1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK_VEC) TRAINDIR=nw_bn_train1-RY TESTDIR=nw_bn_test1-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f1.PET+PETwc+PETlsa+VEChm+VECphrase.c2.4

system-PET+PETwc+PETlsa+VEChm+VECphrase2:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK_VEC) TRAINDIR=nw_bn_train2-RY TESTDIR=nw_bn_test2-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f2.PET+PETwc+PETlsa+VEChm+VECphrase.c2.4
	
system-PET+PETwc+PETlsa+VEChm+VECphrase3:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK_VEC) TRAINDIR=nw_bn_train3-RY TESTDIR=nw_bn_test3-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f3.PET+PETwc+PETlsa+VEChm+VECphrase.c2.4
	
system-PET+PETwc+PETlsa+VEChm+VECphrase4:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK_VEC) TRAINDIR=nw_bn_train4-RY TESTDIR=nw_bn_test4-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.indomain-vec-charniak.$(D).f4.PET+PETwc+PETlsa+VEChm+VECphrase.c2.4

system-PET+PETwc+PETlsa+VEChm+VECphrase-bc1:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK_VEC) TRAINDIR=nw_bn-RY TESTDIR=bc1-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.bc1-vec-charniak.$(D).f.PET+PETwc+PETlsa+VEChm+VECphrase.c2.4
	
system-PET+PETwc+PETlsa+VEChm+VECphrase-cts:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK_VEC) TRAINDIR=nw_bn-RY TESTDIR=cts-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.cts-vec-charniak.$(D).f.PET+PETwc+PETlsa+VEChm+VECphrase.c2.4
	
system-PET+PETwc+PETlsa+VEChm+VECphrase-wl:
	mkdir -p $(logDir)
	nohup make t5-tree_vect-c-svm-light-semtk-withMatrix T=5 F=1 TREETYPE=PET+PETwc+PETlsa+VEChm+VECphrase c=2.4 CONF=$(CHARNIAK_ZHANG_WC_SEMTK_VEC) TRAINDIR=nw_bn-RY TESTDIR=wl-RY MATRIX=$(WACKYMATRIX)  > $(logDir)/nohup.wl-vec-charniak.$(D).f.PET+PETwc+PETlsa+VEChm+VECphrase.c2.4


#################

t5-tree-c-svm-light-semtk:
	$(MAKE) all-c-onlyT-svm-sptk F=$(F) L=0.4 TRAINDIR=$(TRAINDIR) TESTDIR=$(TESTDIR) D=$(D) GR=$(GR) TREETYPE=$(TREETYPE) SVMDIR=$(SVM_DIR)/ CONF=$(CONF) c=$(c) 

t5-tree-c-svm-light-semtk-withMatrix:
	$(MAKE) all-c-onlyT-svm-sptk-withMatrix T=$(T) F=$(F) L=0.4 TRAINDIR=$(TRAINDIR) TESTDIR=$(TESTDIR) D=$(D) GR=$(GR) TREETYPE=$(TREETYPE) SVMDIR=$(SVM_DIR) CONF=$(CONF) c=$(c) MATRIX=$(MATRIX)
	
t5-tree_vect-c-svm-light-semtk:
	$(MAKE) all-c-TV-svm-sptk F=$(F) L=0.4 TRAINDIR=$(TRAINDIR) TESTDIR=$(TESTDIR) D=$(D) GR=$(GR) TREETYPE=$(TREETYPE) SVMDIR=$(SVM_DIR)/ CONF=$(CONF) c=$(c) 

t5-tree_vect-c-svm-light-semtk-withMatrix:
	$(MAKE) all-c-TV-svm-sptk-withMatrix T=$(T) F=$(F) L=0.4 TRAINDIR=$(TRAINDIR) TESTDIR=$(TESTDIR) D=$(D) GR=$(GR) TREETYPE=$(TREETYPE) SVMDIR=$(SVM_DIR) CONF=$(CONF) c=$(c) MATRIX=$(MATRIX)


##############################################

all-c-onlyT-svm-sptk:
	$(MAKE) run-train-test OUTDIR=run/semtk-$(TREETYPE)-t5-F$(F)-L$(L)-c$(c)-GR$(GR)-Tr$(TRAINDIR)-Te$(TESTDIR)-$(CONF)-onlyTree PARAMS="-t 5 -F $(F) -L $(L) -c $(c) -m 10000 -u $(EMPTYMATRIX)" FEATS=configuration/features-$(TREETYPE).xml GR=$(GR) CONF=$(CONF)
	$(MAKE) eval OUTDIR=run/semtk-$(TREETYPE)-t5-F$(F)-L$(L)-c$(c)-GR$(GR)-Tr$(TRAINDIR)-Te$(TESTDIR)-$(CONF)-onlyTree FEATS=configuration/features-$(TREETYPE).xml GR=$(GR) CONF=$(CONF)


all-c-onlyT-svm-sptk-withMatrix:
	$(MAKE) run-train-test OUTDIR=run/semtk-$(TREETYPE)-t5-F$(F)-L$(L)-c$(c)-GR$(GR)-Tr$(TRAINDIR)-Te$(TESTDIR)-$(CONF)-onlyTree PARAMS="-t $(T) -F $(F) -L $(L) -c $(c) -m 8000 -u $(MATRIX)" FEATS=configuration/features-$(TREETYPE).xml GR=$(GR) CONF=$(CONF)
	$(MAKE) eval OUTDIR=run/semtk-$(TREETYPE)-t5-F$(F)-L$(L)-c$(c)-GR$(GR)-Tr$(TRAINDIR)-Te$(TESTDIR)-$(CONF)-onlyTree FEATS=configuration/features-$(TREETYPE).xml GR=$(GR) CONF=$(CONF)
	cat run/semtk-$(TREETYPE)-t5-F$(F)-L$(L)-c$(c)-GR$(GR)-Tr$(TRAINDIR)-Te$(TESTDIR)-$(CONF)-onlyTree/models/*stderr*
	
all-c-TV-svm-sptk:
	$(MAKE) run-train-test OUTDIR=run/semtk-$(TREETYPE)-t5-F$(F)-L$(L)-c$(c)-C$(C)-T$(Tc)-S$(S)-GR$(GR)-Tr$(TRAINDIR)-Te$(TESTDIR)-$(CONF)-onlyTree PARAMS="-t 5 -F $(F) -L $(L) -c $(c) -C $(C) -T $(Tc) -S $(S) -m 10000 -u $(EMPTYMATRIX)" FEATS=configuration/features-$(TREETYPE).xml GR=$(GR) CONF=$(CONF)
	$(MAKE) eval OUTDIR=run/semtk-$(TREETYPE)-t5-F$(F)-L$(L)-c$(c)-C$(C)-T$(Tc)-S$(S)-GR$(GR)-Tr$(TRAINDIR)-Te$(TESTDIR)-$(CONF)-onlyTree FEATS=configuration/features-$(TREETYPE).xml GR=$(GR) CONF=$(CONF)


all-c-TV-svm-sptk-withMatrix:
	$(MAKE) run-train-test OUTDIR=run/semtk-$(TREETYPE)-t5-F$(F)-L$(L)-c$(c)-C$(C)-T$(Tc)-S$(S)-GR$(GR)-Tr$(TRAINDIR)-Te$(TESTDIR)-$(CONF)-onlyTree PARAMS="-t $(T) -F $(F) -L $(L) -c $(c) -C $(C) -T $(Tc) -S $(S) -m 8000 -u $(MATRIX)" FEATS=configuration/features-$(TREETYPE).xml GR=$(GR) CONF=$(CONF)
	$(MAKE) eval OUTDIR=run/semtk-$(TREETYPE)-t5-F$(F)-L$(L)-c$(c)-C$(C)-T$(Tc)-S$(S)-GR$(GR)-Tr$(TRAINDIR)-Te$(TESTDIR)-$(CONF)-onlyTree FEATS=configuration/features-$(TREETYPE).xml GR=$(GR) CONF=$(CONF)
	cat run/semtk-$(TREETYPE)-t5-F$(F)-L$(L)-c$(c)-C$(C)-T$(Tc)-S$(S)-GR$(GR)-Tr$(TRAINDIR)-Te$(TESTDIR)-$(CONF)-onlyTree/models/*stderr*


######################################################################


FEATS=configuration/features-$(PET)+linear$(FE).xml
TMP=~/tmp
#D=true #(directed)
#GR=type #or subtype
WRAPPER=./bin/wrapper.sh
run-train-test:
	echo "TRAINDIR:", $(TRAINDIR)
	echo "TESTDIR:",$(TESTDIR)
	rm -rf $(OUTDIR)
	$(WRAPPER) training $(DATADIR)/$(TRAINDIR) $(DATADIR)/$(TESTDIR) $(SVMDIR) "$(PARAMS)" $(OUTDIR) $(FEATS) $(D) $(GR) configuration/$(CONF)
	$(WRAPPER) test $(DATADIR)/$(TRAINDIR) $(DATADIR)/$(TESTDIR) $(SVMDIR) "$(PARAMS)" $(OUTDIR) $(FEATS) $(D) $(GR) configuration/$(CONF)

eval:
	echo "ALL"
	python3 eval.py $(OUTDIR)/test/out.gold.idx  $(OUTDIR)/test/out.predicted.class






