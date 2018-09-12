#!/usr/bin/env python3
#
# Barbara: reads the nohup file and extracts overall micro-averages
import sys
import math

def main():
    if (len(sys.argv) != 2):
        print("{} nohup".format(sys.argv[0]))
        sys.exit(-1)
    else:
        file = sys.argv[1]
        
        
        GFILE = open(file)

        TP=0
        FP=0
        FN=0
        TN=0

        crosstype=0
        numFold=0

        microTP=0
        microFP=0
        microFN=0

        allF = []
        startRecord=False

        perCoarse={}
        
        for line in GFILE:
            line = line.strip()
            if line.startswith("True positive:"):
                numFold+=1
                fields = line.split()
                TP+= int(fields[2])
            if line.startswith("False positive:"):
                fields= line.split()
                FP+= int(fields[2])
            if line.startswith("False negative:"):
                fields= line.split()
                FN+= int(fields[2])
            if line.startswith("True negative:"):
                fields= line.split()
                TN+= int(fields[2])
            if line.startswith("Crosstype:"):
                fields= line.split()
                crosstype+= int(fields[1])
            if line.startswith("Fscore:"):
                fs = line.split()[1]
                allF.append(float(fs.strip()))
                print("Fscore fold {}: {}".format(numFold,fs))
            if line.startswith("Precision"):
                p = line.split()[2]
                print("Precision: {}".format(p))
            if line.startswith("Recall"):
                r = line.split()[2]
                print("Recall: {}".format(r))
            if line.startswith(">> Micro"):
                fields=line.split()
                microTP+=int(fields[9])
                microFP+=int(fields[11])
                microFN+=int(fields[13])
                print(line)

            if line.startswith("Per coarse-grained"):
                startRecord=True
                continue
            if startRecord and line.startswith("-------"):
                startRecord=False
            if startRecord:
                fields = line.split()
                #print(fields)
                lab = fields[0]
                cTP,cFP,cFN = int(fields[8]),int(fields[10]),int(fields[12])

                if lab in perCoarse:
                    (a,b,c) = perCoarse[lab]
                    perCoarse[lab] = (a+cTP,b+cFP,c+cFN) #update
                else:
                    perCoarse[lab] = (cTP,cFP,cFN)


        GFILE.close()
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

        print("Precision (TP/TP+FP): {0:0.1f}".format(precision))
        print("Recall (TP/TP+FN):    {0:0.1f}".format(recall))
        print("Fscore:               {0:0.1f}".format(fscore))
        print()

        microPrecision=microTP/(microTP+microFP)*100
        microRecall=microTP/(microTP+microFN)*100
        microFscore=(2*microPrecision*microRecall)/(microPrecision+microRecall)

        print("micro-Precision (TP/TP+FP): {0:0.1f}".format(microPrecision))
        print("micro-Recall (TP/TP+FN):    {0:0.1f}".format(microRecall))
        print("micro-Fscore:               {0:0.1f}".format(microFscore))
        print()
        

        for lab in perCoarse:
            (ctp,cfp,cfn) = perCoarse[lab]
            cp = ctp/(ctp+cfp)*100
            cr = ctp/(ctp+cfn)*100
            cf = (2*cp*cr)/(cp+cr)
            print("{0:15} P: {1:>5.1f} R: {2:>5.1f} F: {3:>5.1f}    TP: {4:4} FP: {5:4} FN:{6:4}".format(lab,cp,cr,cf,ctp,cfp,cfn))            
              
        n = len(allF)
        mean = sum(allF)/n
        stdev = math.sqrt(sum((x-mean)**2 for x in allF) / n)
        print("stdev: {}".format(stdev))
        print()
        print("Num folds:  {}".format(numFold))


main()
