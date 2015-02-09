#!/usr/bin/env python3
# author: Barbara Plank
# evaluate multiclass classifier output, assumes "NONE" is label for no-relation
import sys
import codecs

def main():
    if (len(sys.argv) < 3 ):
        print("{} GOLD SYS".format(sys.argv[0]))
        sys.exit(-1)
    else:
        goldFile = sys.argv[1]
        predFile = sys.argv[2]
        classesGold = {}
        classesPred = {}

        countRels=0
        GFILE = codecs.open(goldFile,encoding="utf-8")
        goldLabels = []
        for line in GFILE:
            #            print(line)
            line = line.strip()
            if len(line.split("\t")) > 3:
                key, label, data,rest,sen = line.split("\t")
            else:
                key, label, data = line.split("\t")
            if label.startswith("NONE"):
                label = "NONE"
            else:
                countRels+=1
            # if we only check major type
            if (len(sys.argv)>3 and sys.argv[3] =="major"):
                label = label.split(".")[0]
                
            classesGold[label] = classesGold.get(label,0) + 1
            goldLabels.append(label)
        GFILE.close()

        PFILE = open(predFile)
        predLabels = []
        for line in PFILE:
            line = line.strip()
            if len(line.split("\t")) > 2:
                key, label,rest = line.split("\t")
            else:
                key, label = line.split("\t")
            if (len(sys.argv)>3 and sys.argv[3] =="major"):
                label = label.split(".")[0]
            predLabels.append(label)
            classesPred[label] = classesPred.get(label,0) + 1
        PFILE.close()

        TP=0
        FP=0
        TN=0
        FN=0


        zFP=0
        zFN=0

       # print("goldDistr: ",classesGold)
       # print("predDistr: ",classesPred)
        crosstype=0

        total=0
        correct=0

        perClass ={} #contains [TP,FP,FN]

        #print(perClass)
        microTP=0
        microFP=0
        microFN=0

        #relation detection only
        detTP=0
        detFP=0
        detFN=0


        for g,p in zip(goldLabels,predLabels):
            if g!="NONE":
                if p == g:
                    TP+=1
                elif p == "NONE":
                    FN+=1
                    zFN+=1
                elif p!=g:
                    FP+=1
                    FN+=1
                    crosstype+=1
                    # zhang does not consider this case
                else:
                    print("ERROR!")
                    sys.exit(-1)

            else: 
                # gold is NONE
                if p!="NONE":
                    FP+=1
                    zFP+=1
                elif p=="NONE":
                    TN+=1
                else:
                    print("ERROR")
                    sys.exit(-1)

            # per class
            if g!="NONE":

                if g == p:
                    #TP
                    if perClass.get(g):
                        perClass.get(g)[0]+=1
                    else:
                        perClass[g] = [1,0,0]
                elif p=="NONE":
                    #FN
                    if perClass.get(g):
                        perClass.get(g)[2]+=1
                    else:
                        perClass[g] = [0,0,1]                    
                elif g!=p:
                    #FP for p and FN for g
                    if perClass.get(p):
                        perClass.get(p)[1]+=1
                    else:
                        perClass[p] = [0,1,0]
                    if perClass.get(g):
                        perClass.get(g)[2]+=1
                    else:
                        perClass[g] = [0,0,1]
            else: 
                # g is NONE
                if p != "NONE":
                    # FP
                    if perClass.get(p):
                        perClass.get(p)[1]+=1
                    else:
                        perClass[p] = [0,1,0]


            # relaction detection scores
            if g=="NONE":
                if p!="NONE":
                    detFP+=1
            elif p=="NONE":
                if g!="NONE":
                    detFN+=1
            elif p!="NONE" and g!="NONE":
                detTP+=1

            total+=1
            if g==p:
                correct+=1

        print("Relation Extraction Evaluation tool")
        print("----------------------------------------------------------------------------------")
        print("Performance per class:") 
        coarseClass = []
        for lab in perClass:
            coarse = lab.split(".")[0]
            if not coarse in coarseClass:
                coarseClass.append(coarse)
            if lab != "NONE": #don't consider none
                tup = perClass.get(lab)
                tp=tup[0]
                fp=tup[1]
                fn=tup[2]
            
                microTP+=tp
                microFP+=fp
                microFN+=fn
                if tp != 0:
                    p = tp/(tp+fp)*100
                    r = tp/(tp+fn)*100
                    f = (2*p*r)/(p+r)
                else:
                    p=0
                    r=0
                    f=0
                print("{0:15} P: {1:>5.1f} R: {2:>5.1f} F: {3:>5.1f}    TP: {4:4} FP: {5:4} FN:{6:4}".format(lab,p,r,f,tp,fp,fn))
        print("----------------------------------------------------------------------------------")
        print()
        #print(coarseClass)
        print("Per coarse-grained class (aggregate): ")
        for coarse in coarseClass:
            result=[0,0,0]
            for lab in perClass:
                if lab.startswith(coarse):
                    labScores = perClass.get(lab)
                    result[0] += labScores[0]
                    result[1] += labScores[1]
                    result[2] += labScores[2]
            #print(coarse, result)
            tp=result[0]
            fp=result[1]
            fn=result[2]
            
            if tp != 0:
                p = tp/(tp+fp)*100
                r = tp/(tp+fn)*100
                f = (2*p*r)/(p+r)
            else:
                p=0
                r=0
                f=0
            print("{0:15} P: {1:>5.1f} R: {2:>5.1f} F: {3:>5.1f}    TP: {4:4} FP: {5:4} FN:{6:4}".format(coarse,p,r,f,tp,fp,fn))
        print("----------------------------------------------------------------------------------")
        print()
        microP = microTP/(microTP+microFP)*100
        microR = microTP/(microTP+microFN)*100
        microF = (2*microP*microR)/(microP+microR)
        print("{0:15} P: {1:>5.1f} R: {2:5.1f} F: {3:5.1f}    TP: {4:4} FP: {5:4} FN:{6:4}".format(">> Micro:",microP,microR,microF,microTP,microFP,microFN))
        print("==================================================================================")

        print()
        print("Total relations: {}".format(countRels))
        print()
        print("True positive: {}".format(TP))
        print("True negative: {}".format(TN))
        print("False positive: {}".format(FP))
        print("False negative: {}".format(FN))
        print("Crosstype:      {}".format(crosstype))
        print()
        precision=TP/(TP+FP)*100
        recall=TP/(TP+FN)*100
        fscore=(2*precision*recall)/(precision+recall)
        print("----------------------------------------------------------------------------------")
        print(">> Overall:")
        print("Precision (TP/TP+FP): {0:5.1f}".format(precision))
        print("Recall (TP/TP+FN):    {0:5.1f}".format(recall))
        print("Fscore:               {0:5.1f}".format(fscore))
        print("==================================================================================")
        print()
        print("----------------------------------------------------------------------------------")
        detPrecision=detTP/(detTP+detFP)*100
        detRecall=detTP/(detTP+detFN)*100
        detFscore=(2*detPrecision*detRecall)/(detPrecision+detRecall)
        print(">> Relation Detection only:")
        print("detTrue positive: {}".format(detTP))
        print("detFalse positive: {}".format(detFP))
        print("detFalse negative: {}".format(detFN))
        print("detPrecision: {0:5.1f}".format(detPrecision))
        print("detRecall:    {0:5.1f}".format(detRecall))
        print("detFscore:    {0:5.1f}".format(detFscore))
        print("----------------------------------------------------------------------------------")
        print()
        print("Accuracy:             {0:5.1f}".format(correct/total))

if __name__=="__main__":
    main()
