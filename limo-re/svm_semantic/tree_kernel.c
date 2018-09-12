/***********************************************************************/
/*   FAST TREE KERNEL                                                  */
/*                                                                     */
/*   tree_kernel.c                                                     */
/*                                                                     */
/*   Fast Tree kernels for Support Vector Machines		       */
/*                                                                     */
/*   Author: Alessandro Moschitti 				       */
/*   moschitti@info.uniroma2.it					       */	
/*   Date: 25.10.04                                                    */
/*                                                                     */
/*   Copyright (c) 2004  Alessandro Moschitti - All rights reserved    */
/*                                                                     */
/*   This software is available for non-commercial use only. It must   */
/*   not be modified and distributed without prior permission of the   */
/*   author. The author is not responsible for implications from the   */
/*   use of this software.                                             */
/*                                                                     */
/***********************************************************************/

# include "svm_common.h"

typedef struct nodepair {
  TreeNode    *Nx;	               
  TreeNode    *Nz;	               
} nodePair;


double LAMBDA;
double SIGMA;

double delta_matrix[MAX_NUMBER_OF_NODES][MAX_NUMBER_OF_NODES];


// local functions


double choose_second_kernel(KERNEL_PARM *kernel_parm, DOC *a, DOC *b) ;
double choose_tree_kernel(KERNEL_PARM *kernel_parm, DOC *a, DOC *b);
double sequence(KERNEL_PARM * kernel_parm, DOC * a, DOC * b);
double AVA(KERNEL_PARM * kernel_parm, DOC * a, DOC * b);
double AVA_tree_kernel(KERNEL_PARM * kernel_parm, DOC * a, DOC * b);
double sequence_tree_kernel(KERNEL_PARM * kernel_parm, DOC * a, DOC * b);
void evaluateNorma(KERNEL_PARM * kernel_parm, DOC * d);
double basic_kernel(KERNEL_PARM *kernel_parm, DOC *a, DOC *b, int i, int j);
double tree_kernel(KERNEL_PARM * kernel_parm, DOC * a, DOC * b, int i, int j);
void determine_sub_lists(FOREST *a, FOREST *b, nodePair *intersect, int *n);
double evaluateParseTreeKernel(nodePair *pairs, int n);
double Delta( TreeNode * Nx, TreeNode * Nz);
double advanced_kernels(KERNEL_PARM * kernel_parm, DOC * a, DOC * b);
double sequence_ranking(KERNEL_PARM * kernel_parm, DOC * a, DOC * b, int memberA, int memberB);//all_vs_all vectorial kernel


// Tree Kernels


double Delta( TreeNode * Nx, TreeNode * Nz){
    int i;
    double prod=1;
   
//printf("Delta Matrix: %1.30lf node1:%s node2:%s, LAMBDA %lf, SIGMA %lf\n",delta_matrix[Nx->nodeID][Nz->nodeID],Nx->sName,Nz->sName,LAMBDA,SIGMA);

	if(delta_matrix[Nx->nodeID][Nz->nodeID]>=0) {
//printf("Delta Matrix: %1.30lf è diverso da -1 boh \n",delta_matrix[Nx->nodeID][Nz->nodeID],Nx->sName,Nz->sName,LAMBDA,SIGMA);

	      return delta_matrix[Nx->nodeID][Nz->nodeID]; // Case 0 (Duffy and Collins 2002);
       }
	else 
	   if(Nx->pre_terminal || Nz->pre_terminal)
	    if(strcmp(Nx->production,Nz->production)==0)return (delta_matrix[Nx->nodeID][Nz->nodeID]=LAMBDA);
	      else if(Nx->pre_terminal && Nz->pre_terminal){
	               if((strncmp(Nx->pChild[0]->sName,"000000",6)==0 && 
	                  strncmp(Nz->pChild[0]->sName,"000000",6)==0) || 
	                  (strncmp(Nx->pChild[0]->sName,"+000000",7)==0 && 
	                  strncmp(Nz->pChild[0]->sName,"+000000",7)==0))
	                  return (delta_matrix[Nx->nodeID][Nz->nodeID]=
	      	                //  LAMBDA*(strcmp(Nx->production,Nz->production)==0)); // case 1 
	      	                 LAMBDA*getSimilarity(atoi(Nx->pChild[0]->sName),atoi(Nz->pChild[0]->sName)));
	      	       else return (delta_matrix[Nx->nodeID][Nz->nodeID]=0);
	      	   }
	           else return (delta_matrix[Nx->nodeID][Nz->nodeID]=0);  
	   else{
		   for(i=0;i<Nx->iNoOfChildren;i++)
	   	       if(strcmp(Nx->pChild[i]->production,Nz->pChild[i]->production)==0)
		          prod*= (SIGMA+Delta( Nx->pChild[i], Nz->pChild[i])); // case 2

       		   return (delta_matrix[Nx->nodeID][Nz->nodeID]=LAMBDA*prod);
	     }
}

double evaluateParseTreeKernel(nodePair *pairs, int n){

    int i;
    double sum=0,contr;

	   for(i=0;i<n;i++)
  //     if(strcmp(pairs[i].Nx->sName,"BOX")!= 0 && strcmp(pairs[i].Nz->sName,"BOX")!= 0)
       {
		contr=Delta(pairs[i].Nx,pairs[i].Nz);
//		printf("Score for the pairs (%s , %s): %f\n",pairs[i].Nx->sName,pairs[i].Nz->sName,contr);fflush(stdout);
		sum+=contr;
	   }
//   printf("FORM EVALUATE KERNEL = %f \n\n\n",sum); 

	   return sum;
}

void determine_sub_lists(FOREST *a, FOREST *b, nodePair *intersect, int *n){

   int i=0,j=0,j_old,j_final;
   int n_a,n_b;
   short cfr;
   OrderedTreeNode *list_a, *list_b;

   n_a = a->listSize;
   n_b = b->listSize;
   list_a=a->orderedNodeSet; 
   list_b=b->orderedNodeSet;
   *n=0;
   

   //  TEST
   /*    printf("\n\n\nLenghts %d %d\n",a->listSize , b->listSize);
    printf("LIST1:\n");
    for(i=0;i<a->listSize;i++) printf("%s\n",a->orderedNodeSet[i].sName);
    printf("\n\n\nLIST2:\n");
    for(i=0;i<b->listSize;i++) printf("%s\n",b->orderedNodeSet[i].sName);
    i=0;
    printf("Determining LISTS:\n");fflush(stdout);
*/
    while(i<n_a && j<n_b){
      if((cfr=strcmp(list_a[i].sName,list_b[j].sName))>0)j++;
      else if(cfr<0)i++;
	   else{
		j_old=j;
		do{
		  do{
		    intersect[*n].Nx=list_a[i].node;
		    intersect[*n].Nz=list_b[j].node;
		    (*n)++;
		    delta_matrix[list_a[i].node->nodeID][list_b[j].node->nodeID]=-1.0;		    
//  TEST //         printf("Evaluating-Pair: (%s  ,  %s) i %d,j %d j_old%d \n",list_a[i].sName,list_b[j].sName,i,j,j_old);fflush(stdout);
		    j++;
		  }
		  while(j<n_b && (strcmp(list_a[i].sName,list_b[j].sName)==0)
		  	);
		  i++;j_final=j;j=j_old;
		} 		
	        while(i<n_a && strcmp(list_a[i].sName,list_b[j].sName)==0);
		j=j_final;
	      }
   }
   if (*n>MAX_NUMBER_OF_PAIRS) { printf ("ERROR: The number of identical parse nodes exceed the current capacityn\n"); exit(-1);}
}


double tree_kernel(KERNEL_PARM * kernel_parm, DOC * a, DOC * b, int i, int j){

  int n_pairs=0;

  double k=0;
  
  nodePair intersect[MAX_NUMBER_OF_PAIRS];
   
  if (b->num_of_trees > j && a->num_of_trees > i){
  
       //  printf("\n tree 1: <"); writeTreeString(a->forest_vec[i]->root);printf(">\n");
  
        //  printf("\n tree 2: <"); writeTreeString(b->forest_vec[i]->root);printf(">\n"); fflush(stdout);

         determine_sub_lists(a->forest_vec[i],b->forest_vec[j],intersect,&n_pairs);
         k =(evaluateParseTreeKernel(intersect,n_pairs))/
              sqrt(a->forest_vec[i]->twonorm_PT*b->forest_vec[i]->twonorm_PT);
         //printf("\n\n(i,j)=(%d,%d)= Kernel :%f norm1,norm2 (%f,%f)\n",i,j,k,a->forest_vec[i]->twonorm_PT, b->forest_vec[j]->twonorm_PT);
  } 
  else{
       printf("\nERROR: attempting to access to a tree not defined in the data\n\n");
       exit(-1);
  }
  
  return k;
}


double basic_kernel(KERNEL_PARM *kernel_parm, DOC *a, DOC *b, int i, int j) 
     /* calculate the kernel function */
{
  switch(kernel_parm->second_kernel) {
    case 0: /* linear */ 
            return(sprod_i(a, b, i, j)); 
    case 1: /* polynomial */
            return(pow(kernel_parm->coef_lin*sprod_i(a, b, i, j)
                                +kernel_parm->coef_const,(double)kernel_parm->poly_degree)); 
    case 2: /* radial basis function */
            return(exp(-kernel_parm->rbf_gamma*(a->vectors[i]->twonorm_sq-2*sprod_i(a, b, i, j)+b->vectors[i]->twonorm_sq)));
    case 3: /* sigmoid neural net */
            return(tanh(kernel_parm->coef_lin*sprod_i(a, b, i, j)+kernel_parm->coef_const)); 
    case 4: /* custom-kernel supplied in file kernel.h*/
            return(custom_kernel(kernel_parm,a,b));
    case 5: /* TREE KERNEL */
            return(tree_kernel(kernel_parm,a,b,i,j));
            	     
    default: printf("Error: The kernel function to be combined with the Tree Kernel is unknown\n"); exit(1);
  }
}


void evaluateNorma(KERNEL_PARM * kernel_parm, DOC * d){

    int n_pairs=0,
        i;
        
    double k=0;
    nodePair intersect[MAX_NUMBER_OF_PAIRS];

//printf("doc ID :%d \n",d->docnum);
//printf("num of vectors:%d \n",d->num_of_vectors);
//fflush(stdout);

    
      for(i=0;i < d->num_of_trees;i++){
      /*  TESTS 

          printf("\n\n\nnode ID: %d \n", d->forest_vec[i]->root->nodeID); fflush(stdout);

          printf("node list length: %d\n", d->forest_vec[i]->listSize);

          printf("doc ID :%d \n",d->docnum);

          printf("tree: <"); writeTreeString(d->forest_vec[i]->root);printf(">");
    
          printf("\n\n"); fflush(stdout);*/

          // this avoids to check for norm == 0
          //printf ("Norm %f\n",k1);

          determine_sub_lists(d->forest_vec[i],d->forest_vec[i],intersect,&n_pairs);
          k =(evaluateParseTreeKernel(intersect,n_pairs));
                    
          if(k!=0 && (kernel_parm->normalization == 1 || kernel_parm->normalization == 3)) 
               d->forest_vec[i]->twonorm_PT=k; 
          else d->forest_vec[i]->twonorm_PT=1; 
      }
  /* SECOND KERNEL NORM EVALUATION */

      for(i=0;i < d->num_of_vectors;i++){
//      for(i=0;i < 61 && i<d->num_of_vectors;i+=60){
        
          d->vectors[i]->twonorm_STD=1; // basic-kernel normalizes the standard kernels
                                        // this also avoids to check for norm == 0
                                        
          k = basic_kernel(kernel_parm, d, d, i, i);
                   
          if(k!=0 && (kernel_parm->normalization == 2 || kernel_parm->normalization == 3))
               d->vectors[i]->twonorm_STD=k; // if selected normalization is applied
               
          d->vectors[i]->twonorm_sq=sprod_ss(d->vectors[i]->words,d->vectors[i]->words);
       } 
      // maintain the compatibility with svm-light single linear vector 
        if(d->num_of_vectors>0) d->twonorm_sq=sprod_ss(d->vectors[0]->words,d->vectors[0]->words);
        else d->twonorm_sq=0;
 }


                           /*******************************/
                           /* KERNELS OVER SET OF KERNELS */
                           /*******************************/
                           
                           
 double Semantic_Tree_kernel(KERNEL_PARM * kernel_parm, DOC * a, DOC * b){//all_vs_all_tree_kernel

  int i,j;
  double k,k0,k1,k2,k3,k4,k15,k16,k20;
  int n_pairs=0;
  nodePair intersect[MAX_NUMBER_OF_PAIRS];
  

  k0=k1=k2=k3=k4=k16=k20=k15=0;

//   printf("\n\nDocum %ld and %ld, size=(%d,%d)\n",a->docnum,b->docnum,a->num_of_trees,b->num_of_trees);
   k=0;

  //for(i=3; i< a->num_of_trees && i< b->num_of_trees; i++)
   {
        /* 
         printf("\n\n\n nodes: %d  %d\n", a->forest_vec[i]->root->nodeID,b->forest_vec[i]->root->nodeID);
         printf("node list lenghts: %d  %d\n", a->forest_vec[i]->listSize,b->forest_vec[i]->listSize);
         */
   //      printf("\ntree 1: <"); writeTreeString(a->forest_vec[i]->root);
  
    //     printf(">\ntree 2: <"); writeTreeString(b->forest_vec[i]->root);printf(">\n"); fflush(stdout);


	  //  LAMBDA = .4;
	
	/**
  	* KERNEL FOR QUESTION CLASSIFICATION
	* Old format:
	* 0 PT, 1 BOW, 2 POS, 3 pairs 4 BONT , 5-9 PAS, 10-14 AST
	* New format:
	* contains up to 5 PAS and up to 5 AST at the end of the string
	* Question: 
	* 0: PT, 1: BOW, 2: POS, 3=PAIRS, 4=BONT, 5-9 PAS, 10-14=AST
	* best combo should be PT+BOW+POS
    
	
	for(i=0; i< a->num_of_trees && i< b->num_of_trees ; i++)	
	  if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL && (i ==0
															  || i == 1
															   ||i==2)
															  ){// || i == 0 || i==3 || i==4)){
        		 determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         		k2+= (evaluateParseTreeKernel(intersect,n_pairs)/
        			sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
      		}
      		
      		*/
	
	//PAS
	
	
	i=0;
	if (i>= a->num_of_trees || i>= b->num_of_trees) return 0;
/*printf("\ntree 1: "); writeTreeString(a->forest_vec[i]->root);
printf("\ntree 2: "); writeTreeString(b->forest_vec[i]->root);
printf("\n"); fflush(stdout);
*/
         if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){                                
         determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k0= (evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
         } 
	 		
	i=1;
	if (i>= a->num_of_trees || i>= b->num_of_trees) return 0;
         if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){                                
         determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k1 = (evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
	 } 
/*	 
	 	i=5;
	if (i>= a->num_of_trees || i>= b->num_of_trees) return 0;
         if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){                                
         determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k4= (evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
         } 
  
	 
	i=2;
	if (i>= a->num_of_trees || i>= b->num_of_trees) return 0;
         if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){                                
         determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k3= (evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
         } 
	
	

        
  	if(a->forest_vec[i]->root!=NULL && b->forest_vec[i]->root!=NULL){
          printf("\ntree 1: <"); writeTreeString(a->forest_vec[i]->root);
          printf(">\ntree 2: "); writeTreeString(b->forest_vec[i]->root);printf(">\n");
   	}
        
         printf("\n\n(i,i)=(%d,%d)= Kernel-Sequence :%f norm1,norm2 (%f,%f)\n",i,i,k2,a->forest_vec[i]->twonorm_PT, b->forest_vec[i]->twonorm_PT);
	*/
		 
		 
	
		 /*
	for(i=5; i < a->num_of_trees; i++)
   		for(j=i; j<7 && j< b->num_of_trees; j++)
		  if(a->forest_vec[i]!=NULL && b->forest_vec[j]!=NULL){ // && i==j
       				determine_sub_lists(a->forest_vec[i],b->forest_vec[j],intersect,&n_pairs);
        			k2+= (evaluateParseTreeKernel(intersect,n_pairs)/
	      			sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[j]->twonorm_PT));
					}
		 */

	/*
	i=20; if (i>= a->num_of_trees || i>= b->num_of_trees) return 0;
         if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){ 
         determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k1+= (evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
	 }  
	*/


	/* //KERNEL FOR QUESTION DATA
   	for(i=0; i< a->num_of_trees && i< b->num_of_trees && i < 3 ; i++)
         if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){
               determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k2+= (evaluateParseTreeKernel(intersect,n_pairs)/
	                sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
         }
	*/


 /*
//USE SAME PROCEDURE TO ADD A KERNEL WITH POSITION i
 //a's sentence- b's sentence (as in znznzn)
i=3; if (i>= a->num_of_trees || i>= b->num_of_trees) return 0;
         if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){                                
         determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k1+= (evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
		 } 
*/

/**
* KERNEL FOR (ONE SENTENCE) PARAGRAPH CLASSIFICATION
* Old format: 0: PT, 1: PAS, 2: AST, 3=BOW, 4=POS, 5=PAIRS, 6=BONT
* New format:
* contains up to 5 PAS and up to 5 AST at the end of the string
* Question: 
* 0: PT, 1: BOW, 2: POS, 3=PAIRS, 4=BONT, 5-9 PAS, 10-14=AST
* Sentence: 
* 15: PT, 16: BOW, 17: POS, 18=PAIRS, 19=BONT, 20-24 PAS, 25-29=AST
*/
/*
if(a->num_of_trees != 30 && a->num_of_trees != 0 ){
	printf("a nbTrees = %d\n",a->num_of_trees);
	for(i=0; i< a->num_of_trees; i++)
		writeTreeString(a->forest_vec[i]->root);
		printf("\n");
	
	}
*/
/*
//0: PT, 1=BOW, 2=POS, 3=PAIRS, 4=BONT, 5-9: PAS, 10-14: AST
//Comparing a's question parse tree with b's question parse tree (as in znznzn):
i=0; if (i>= a->num_of_trees || i>= b->num_of_trees) return 0;
         if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){ 
         determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k1+= (evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
	 }  
*/
/*
//Sentence features: 
//15: PT, 16: BOW, 17: POS, 18=PAIRS, 19=BONT, 20-24 PAS, 25-29=AST
//Comparing a's sentence features with b's sentence features:
for(i=15; i< a->num_of_trees && i< b->num_of_trees && i<20; i++)
      if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL  && (i ==15 || i==16 || i==17)){//paragraphs // || i==16 || i==17
         determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k1+= (evaluateParseTreeKernel(intersect,n_pairs)/
	      sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
	 }
	 
i=20; if (i>= a->num_of_trees || i>= b->num_of_trees) return 0;
         if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){ 
         determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k1+= (evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
	 }  
	 
	 i=21; if (i>= a->num_of_trees || i>= b->num_of_trees) return 0;
         if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){ 
         determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k1+= (evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
	 }  
	
*/
//PAS di seguito (20-24) 
//Comparing a's sentence features with b's sentence features:
/*
for(i=20; i<25 && i < a->num_of_trees; i++)
   for(j=i; j<25 && j< b->num_of_trees; j++)
      if(a->forest_vec[i]!=NULL && b->forest_vec[j]!=NULL && i==j){ // && i==j
         determine_sub_lists(a->forest_vec[i],b->forest_vec[j],intersect,&n_pairs);
         k2+= (evaluateParseTreeKernel(intersect,n_pairs)/
	      sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[j]->twonorm_PT));
	      
          if(a->forest_vec[i]->root!=NULL && b->forest_vec[j]->root!=NULL){
          	printf("\ntree 1: <"); writeTreeString(a->forest_vec[i]->root);
          	printf(">\ntree 2: "); writeTreeString(b->forest_vec[j]->root);printf(">\n");
   	   }
        
           printf("\n\n(i,j)=(%d,%d)= Kernel-Sequence :%f norm1,norm2 (%f,%f)\n",i,j,k2,a->forest_vec[i]->twonorm_PT, b->forest_vec[j]->twonorm_PT);

	 }
	 
k2=k2;
*/

/*
//AST di seguito (25-29)
//Comparing a's sentence features with b's sentence features:
for(i=25; i<30 && i < a->num_of_trees; i++)
   for(j=25; j<30 && j< b->num_of_trees; j++)
      if(a->forest_vec[i]!=NULL && b->forest_vec[j]!=NULL){
         determine_sub_lists(a->forest_vec[i],b->forest_vec[j],intersect,&n_pairs);
         k2+= (evaluateParseTreeKernel(intersect,n_pairs)/
	      sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[j]->twonorm_PT));
	 }
*/
 /*
//Crossing question features with sentence features:
//cambiare a seconda del formato a 3 o 7 slots!!!!
	// a's question with b' sentence
	for(i=0; i< a->num_of_trees && i< 7; i++)
     		for(j=3; j< b->num_of_trees; j++)
         		if(a->forest_vec[i]!=NULL && b->forest_vec[j]!=NULL && (i % 7 == j % 7)  && (i % 7 ==0 || i % 7 ==1 || i % 7 ==3 || i % 7 ==4)){
          			determine_sub_lists(a->forest_vec[i],b->forest_vec[j],intersect,&n_pairs);
           			k2+= (evaluateParseTreeKernel(intersect,n_pairs)/
           				sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[j]->twonorm_PT));
			}
	//b's question with a's sentence
      	for(i=0; i< b->num_of_trees && i< 7; i++)
        	for(j=7; j< a->num_of_trees; j++)
            		if(b->forest_vec[i]!=NULL && a->forest_vec[j]!=NULL && (i % 7 == j % 7) && (i % 7 ==0 || i % 7 ==1 || i % 7 ==3 || i % 7 ==4)){
           			determine_sub_lists(b->forest_vec[i],a->forest_vec[j],intersect,&n_pairs);
            			k2+= (evaluateParseTreeKernel(intersect,n_pairs)/
           				 sqrt(b->forest_vec[i]->twonorm_PT * a->forest_vec[j]->twonorm_PT));
         		}

*/
//endof Kernel for paragraph classification
/*
i=3;
     while(i< a->num_of_trees && i< b->num_of_trees){
	 if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL && i % 3 != 2 && i % 3 != 1 ){                                
            determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
            k2+= (evaluateParseTreeKernel(intersect,n_pairs)/
                 sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
         }
	 i++;
     }

// LAMBDA = 1;

 
i=3; if (i>= a->num_of_trees || i>= b->num_of_trees) return 0;

         if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){                                
         determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k3 = (evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
      }
*/
// best kernel k = k1 + k2 + k1*k3 + k2*k3 + k1*k2;
// best kernel k = k1 + k2 + k1*k3 + k2*k3 + k1*k2 + k4*k1*k2 with k4;
/*
	i=16;
	if (i>= a->num_of_trees || i>= b->num_of_trees) return 0;
	if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){                                
	  determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
	  k16 = (evaluateParseTreeKernel(intersect,n_pairs)/
			 sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
	}
	
	
	i=15;
	if (i>= a->num_of_trees || i>= b->num_of_trees) return 0;
	if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){                                
	determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
	k15 = (evaluateParseTreeKernel(intersect,n_pairs)/
	sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
	}
	
	k20=0;
	for(i=20; i<23 && i < a->num_of_trees; i++)
	for(j=20; j<23 && j< b->num_of_trees; j++)
	if(a->forest_vec[i]!=NULL && b->forest_vec[j]!=NULL){
	determine_sub_lists(a->forest_vec[i],b->forest_vec[j],intersect,&n_pairs);
	k20+= (evaluateParseTreeKernel(intersect,n_pairs)/
	sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[j]->twonorm_PT));//((i+j-38)*(i+j-38));
	}
*/	
	
k = k1+k0;//k1 + k16  + k20*kernel_parm->tree_constant; //+ k1*k2 ; //+ k4*k2;
//+ k0 + k15 

   }
   return k;
}




double sequence_tree_kernel(KERNEL_PARM * kernel_parm, DOC * a, DOC * b){//all_vs_all_tree_kernel

  int i;
  double k;
  int n_pairs=0;
  nodePair intersect[MAX_NUMBER_OF_PAIRS];
  
//   printf("\n\nDocum %ld and %ld, size=(%d,%d)\n",a->docnum,b->docnum,a->num_of_trees,b->num_of_trees);
   k=0;

   for(i=0; i< a->num_of_trees && i< b->num_of_trees; i++){
        /* 
         printf("\n\n\n nodes: %d  %d\n", a->forest_vec[i]->root->nodeID,b->forest_vec[i]->root->nodeID);
         printf("node list lenghts: %d  %d\n", a->forest_vec[i]->listSize,b->forest_vec[i]->listSize);
         
         printf("\ntree 1: <"); writeTreeString(a->forest_vec[i]->root);printf(">\n");
  
         printf("\ntree 2: <"); writeTreeString(b->forest_vec[i]->root);printf(">\n"); fflush(stdout);
*/
      if(a->forest_vec[i]!=NULL && b->forest_vec[i]!=NULL){                                
         determine_sub_lists(a->forest_vec[i],b->forest_vec[i],intersect,&n_pairs);
         k+= (evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[i]->twonorm_PT));
      }

 //        printf("\n\n(i,i)=(%d,%d)= Kernel-Sequence :%f norm1,norm2 (%f,%f)\n",i,i,k,a->forest_vec[i]->twonorm_PT, b->forest_vec[i]->twonorm_PT);
  }
   return k;
}


double AVA_tree_kernel(KERNEL_PARM * kernel_parm, DOC * a, DOC * b){//all_vs_all_tree_kernel

  int i,
      j,
      n_pairs=0;

  double k=0;
  nodePair intersect[MAX_NUMBER_OF_PAIRS];
   
   //printf("first elements: %s  %s\n", a->orderedNodeSet->sName,b->orderedNodeSet->sName);
   //printf("\n\n---------------------------------------------------------\n\n");fflush(stdout);
   //printf("doc IDs :%d %d",a->docnum,b->docnum);

   if (b->num_of_trees == 0 || a->num_of_trees==0) return 0;
   
   for(i=0; i< a->num_of_trees; i++)
      for(j=0; j< b->num_of_trees;j++){
  
      if(a->forest_vec[i]!=NULL && b->forest_vec[j]!=NULL){

        /* 
         printf("\n\n\n nodes: %d  %d\n", a->forest_vec[i]->root->nodeID,b->forest_vec[i]->root->nodeID);
         printf("node list lenghts: %d  %d\n", a->forest_vec[i]->listSize,b->forest_vec[i]->listSize);
         */
        //printf("\ntree 1: <"); writeTreeString(a->forest_vec[i]->root);
  
        //printf(">\ntree 2: "); writeTreeString(b->forest_vec[j]->root);printf(">\n");
        //fflush(stdout);

         determine_sub_lists(a->forest_vec[i],b->forest_vec[j],intersect,&n_pairs);

         k+= (evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[j]->twonorm_PT));
         //printf("\n\n(i,j)=(%d,%d)= Kernel :%f norm1,norm2 (%f,%f)\n",i,j,k,a->forest_vec[i]->twonorm_PT, b->forest_vec[j]->twonorm_PT);
      }
     }
    //printf("\n---------------------------------------------------------------\n"); fflush(stdout);
  

   return k;
}


double sequence(KERNEL_PARM * kernel_parm, DOC * a, DOC * b){//all_vs_all vectorial kernel

  int i;
  double k=0;
    
   for(i=0; i< a->num_of_vectors && i< b->num_of_vectors; i++)
     
      if(a->vectors[i]!=NULL && b->vectors[i]!=NULL){
         k+= basic_kernel(kernel_parm, a, b, i, i)/
         sqrt(a->vectors[i]->twonorm_STD * b->vectors[i]->twonorm_STD);
      }
   return k;
}

double instance_weight(KERNEL_PARM * kernel_parm, DOC * a, DOC * b){//all_vs_all vectorial kernel
  /* B: instance weighting. We store weight of instance in feature vector -- thus use only one, don't multiply! */
  int i;
  double k=0; /* default is zero!  */
  WORD *ai;
  //printf("a->num_of_vectors: %d\n",a->num_of_vectors);
  //assume we have only a and just one feature!!  
  i=0;
  if (a->num_of_vectors==1) {
      if(a->vectors[i]!=NULL){
	ai = a->vectors[i]->words;
	k+= ai->weight;
      }

  } //else {
  //  printf("ERROR: instance weighting - vector has more than one feature!\n");
  //}
  //printf("instance_weight: %f\n",k);
   return k;
}


double AVA(KERNEL_PARM * kernel_parm, DOC * a, DOC * b){//all_vs_all vectorial kernel

  int i,
      j;
      
  double k=0;
    
   for(i=0; i< a->num_of_vectors; i++)
      for(j=0; j< b->num_of_vectors;j++){
  
         if(a->vectors[i]!=NULL && b->vectors[j]!=NULL){
             k+= basic_kernel(kernel_parm, a, b, i, j)/
             sqrt(a->vectors[i]->twonorm_STD * b->vectors[j]->twonorm_STD);
         }
      }
  return k;
}



double ACL2005_Entailment_kernel(KERNEL_PARM * kernel_parm, DOC * a, DOC * b){

  int i,
      n_pairs=0;

  double k=0,
         max=0;

  nodePair intersect[MAX_NUMBER_OF_PAIRS];
   
   LAMBDA = kernel_parm->lambda; //faster access for lambda
   SIGMA = kernel_parm->sigma;

       //  printf("first elements: %s  %s\n", a->orderedNodeSet->sName,b->orderedNodeSet->sName);
   //printf("\n\n---------------------------------------------------------\n\n");fflush(stdout);
   //printf("doc IDs :%d %d",a->docnum,b->docnum);

 if (b->num_of_trees > 1 && a->num_of_trees>1){
   //kk=0;
   //for(i=0; i< 2; i++)
   //  for(j=0; j< 2;j++){
   //   if(a->forest_vec[i]!=NULL && b->forest_vec[j]!=NULL){
        /* 
         printf("\n\n\n nodes: %d  %d\n", a->forest_vec[i]->root->nodeID,b->forest_vec[i]->root->nodeID);
         printf("node list lenghts: %d  %d\n", a->forest_vec[i]->listSize,b->forest_vec[i]->listSize);
         */
        //printf("\ntree 1: <"); writeTreeString(a->forest_vec[i]->root);
        //printf(">\ntree 2: "); writeTreeString(b->forest_vec[j]->root);printf(">\n");
        //fflush(stdout);
         //determine_sub_lists(a->forest_vec[i],b->forest_vec[j],intersect,&n_pairs);
         //kk+= (evaluateParseTreeKernel(intersect,n_pairs)/sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[j]->twonorm_PT));
         //printf("\n\n(i,j)=(%d,%d)= Kernel :%f norm1,norm2 (%f,%f)\n",i,j,k,a->forest_vec[i]->twonorm_PT, b->forest_vec[j]->twonorm_PT);
    //  }
    // }
        // determine_sub_lists(a->forest_vec[0],b->forest_vec[0],intersect,&n_pairs);
        // kk+= (evaluateParseTreeKernel(intersect,n_pairs)/sqrt(a->forest_vec[0]->twonorm_PT * b->forest_vec[0]->twonorm_PT));
        // determine_sub_lists(a->forest_vec[1],b->forest_vec[1],intersect,&n_pairs);
        // kk+= (evaluateParseTreeKernel(intersect,n_pairs)/sqrt(a->forest_vec[1]->twonorm_PT * b->forest_vec[1]->twonorm_PT));
   }
   //kk = 0; FMZ aggiunto per osservare il comporatmento solo del max
   
   max = 0;
   if (b->num_of_trees > 2  && a->num_of_trees > 2){ 
   if (b->num_of_trees >  a->num_of_trees) {  
     for(i=2;i<b->num_of_trees ;i+=2){
      if(a->forest_vec[2]!=NULL && b->forest_vec[i]!=NULL){
          //  printf("\n\n\n nodes: %d  %d\n", a->forest_vec[i]->root->nodeID,b->forest_vec[i]->root->nodeID);
        //  printf("node list lenghts: %d  %d\n", a->forest_vec[i]->listSize,b->forest_vec[i]->listSize);
          // printf("\ntree 1: "); writeTreeString(a->forest_vec[i]->root);
          //printf("\ntree 2: "); writeTreeString(b->forest_vec[i]->root);
         // fflush(stdout);
         determine_sub_lists(a->forest_vec[2],b->forest_vec[i],intersect,&n_pairs);
	 k= evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[2]->twonorm_PT * b->forest_vec[i]->twonorm_PT);
         determine_sub_lists(a->forest_vec[3],b->forest_vec[i+1],intersect,&n_pairs);
         k+= evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[3]->twonorm_PT * b->forest_vec[i+1]->twonorm_PT);
         if(max<k)max=k;
         //printf("\n\nKernel :%f \n",k);
       } 
      }
     } else {
	     
      for(i=2;i<a->num_of_trees ;i+=2){
       if(a->forest_vec[i]!=NULL && b->forest_vec[2]!=NULL){
          //  printf("\n\n\n nodes: %d  %d\n", a->forest_vec[i]->root->nodeID,b->forest_vec[i]->root->nodeID);
        //  printf("node list lenghts: %d  %d\n", a->forest_vec[i]->listSize,b->forest_vec[i]->listSize);
          // printf("\ntree 1: "); writeTreeString(a->forest_vec[i]->root);
          //printf("\ntree 2: "); writeTreeString(b->forest_vec[i]->root);
         // fflush(stdout);
         determine_sub_lists(a->forest_vec[i],b->forest_vec[2],intersect,&n_pairs);
	     k= evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i]->twonorm_PT * b->forest_vec[2]->twonorm_PT);
         determine_sub_lists(a->forest_vec[i+1],b->forest_vec[3],intersect,&n_pairs);
         k+= evaluateParseTreeKernel(intersect,n_pairs)/
         sqrt(a->forest_vec[i+1]->twonorm_PT * b->forest_vec[3]->twonorm_PT);
         if(max<k)max=k;
         //printf("\n\nKernel :%f \n",k);
       }
      }
     }
    }
   //printf("\n---------------------------------------------------------------\n");fflush(stdout);
printf("\n\nKernel :%f \n",max);

  if(kernel_parm->combination_type=='+' && (a->vectors!=NULL && b->vectors!=NULL))
       return basic_kernel(kernel_parm, a, b, 0, 0)/
              sqrt(a->vectors[0]->twonorm_STD * b->vectors[0]->twonorm_STD)+ 
              kernel_parm->tree_constant*max;
  else return max;
}



double SRL_re_ranking_CoNLL2006(KERNEL_PARM * kernel_parm, DOC * a, DOC * b){//all_vs_all_tree_kernel

  int n_pairs=0;

  double k1=0,k2=0;
  nodePair intersect[MAX_NUMBER_OF_PAIRS];
   
      //printf("first elements: %s  %s\n", a->orderedNodeSet->sName,b->orderedNodeSet->sName);
   //printf("\n\n---------------------------------------------------------\n\n");fflush(stdout);
  
 if(kernel_parm->kernel_type==11 || kernel_parm->kernel_type==12){

   if(a->num_of_trees!=0 && b->num_of_trees!= 0){

      if(a->forest_vec[0]==NULL || a->forest_vec[1]==NULL
         || b->forest_vec[0]==NULL || b->forest_vec[1]==NULL){
         printf("ERROR: two trees for each instance are needed");
         exit(-1);
      }

         determine_sub_lists(a->forest_vec[0],b->forest_vec[0],intersect,&n_pairs);
         k1+= evaluateParseTreeKernel(intersect,n_pairs)/sqrt(a->forest_vec[0]->twonorm_PT * b->forest_vec[0]->twonorm_PT);

         determine_sub_lists(a->forest_vec[1],b->forest_vec[1],intersect,&n_pairs);
         k1+= evaluateParseTreeKernel(intersect,n_pairs)/sqrt(a->forest_vec[1]->twonorm_PT * b->forest_vec[1]->twonorm_PT);

         determine_sub_lists(a->forest_vec[1],b->forest_vec[0],intersect,&n_pairs);
         k1-= evaluateParseTreeKernel(intersect,n_pairs)/sqrt(a->forest_vec[1]->twonorm_PT * b->forest_vec[0]->twonorm_PT);

         determine_sub_lists(a->forest_vec[0],b->forest_vec[1],intersect,&n_pairs);
         k1-= evaluateParseTreeKernel(intersect,n_pairs)/sqrt(a->forest_vec[0]->twonorm_PT * b->forest_vec[1]->twonorm_PT);

         if(kernel_parm->kernel_type==12)k1*=kernel_parm->tree_constant;

    //printf("\n\n(i,j)=(%d,%d)= Kernel :%f norm1,norm2 (%f,%f)\n",i,j,k,a->forest_vec[i]->twonorm_PT, b->forest_vec[j]->twonorm_PT);
    //printf("\n---------------------------------------------------------------\n"); fflush(stdout);
   }
}

// to use all the local argument classifier features as in [Toutanova ACL2005]

if(kernel_parm->kernel_type==13 || kernel_parm->kernel_type==12){
          k2+=sequence_ranking(kernel_parm, a, b, 0, 0);
          k2+=sequence_ranking(kernel_parm, a, b, 1, 1);
          k2-=sequence_ranking(kernel_parm, a, b, 0, 1);
          k2-=sequence_ranking(kernel_parm, a, b, 1, 0);
          }


// use only 1 vector for each predicate argument structure
          
//     if(kernel_parm->kernel_type==13 || kernel_parm->kernel_type==12){
//       if(a->num_of_vectors>0 && b->num_of_vectors>0){
//         k2+=basic_kernel(kernel_parm, a, b, 0, 0)/
//          sqrt(a->vectors[0]->twonorm_STD * b->vectors[0]->twonorm_STD);
//	     k2+=basic_kernel(kernel_parm, a, b, 1, 1)/
//          sqrt(a->vectors[1]->twonorm_STD * b->vectors[1]->twonorm_STD);
//         k2-=basic_kernel(kernel_parm, a, b, 0, 1)/
//          sqrt(a->vectors[0]->twonorm_STD * b->vectors[1]->twonorm_STD);
//         k2-=basic_kernel(kernel_parm, a, b, 1, 0)/
//          sqrt(a->vectors[1]->twonorm_STD * b->vectors[0]->twonorm_STD);
//          }
//       }  



    
   // printf("kernel: %f\n",k2);

   return  k1+k2;//(k1*k2 + k2 + (k1+1)*(k1+1));
}


double tree_kernel_ranking(KERNEL_PARM * kernel_parm, DOC * a, DOC * b){//all_vs_all_tree_kernel

  int n_pairs=0;

  double k=0;
  nodePair intersect[MAX_NUMBER_OF_PAIRS];
   
      //printf("first elements: %s  %s\n", a->orderedNodeSet->sName,b->orderedNodeSet->sName);
   //printf("\n\n---------------------------------------------------------\n\n");fflush(stdout);
  
   if(a->num_of_trees==0 || b->num_of_trees== 0) return 0;
   
      if(a->forest_vec[0]==NULL || a->forest_vec[1]==NULL
         || b->forest_vec[0]==NULL || b->forest_vec[1]==NULL){
         printf("ERROR: two trees for each instance are needed");
         exit(-1);
      }

         determine_sub_lists(a->forest_vec[0],b->forest_vec[0],intersect,&n_pairs);
         k+= evaluateParseTreeKernel(intersect,n_pairs)/sqrt(a->forest_vec[0]->twonorm_PT * b->forest_vec[0]->twonorm_PT);

         determine_sub_lists(a->forest_vec[1],b->forest_vec[1],intersect,&n_pairs);
         k+= evaluateParseTreeKernel(intersect,n_pairs)/sqrt(a->forest_vec[1]->twonorm_PT * b->forest_vec[1]->twonorm_PT);

         determine_sub_lists(a->forest_vec[1],b->forest_vec[0],intersect,&n_pairs);
         k-= evaluateParseTreeKernel(intersect,n_pairs)/sqrt(a->forest_vec[1]->twonorm_PT * b->forest_vec[0]->twonorm_PT);

         determine_sub_lists(a->forest_vec[0],b->forest_vec[1],intersect,&n_pairs);
         k-= evaluateParseTreeKernel(intersect,n_pairs)/sqrt(a->forest_vec[0]->twonorm_PT * b->forest_vec[1]->twonorm_PT);

         //printf("\n\n(i,j)=(%d,%d)= Kernel :%f norm1,norm2 (%f,%f)\n",i,j,k,a->forest_vec[i]->twonorm_PT, b->forest_vec[j]->twonorm_PT);
    //printf("\n---------------------------------------------------------------\n"); fflush(stdout);
           
   return k;
}

double vector_ranking(KERNEL_PARM * kernel_parm, DOC * a, DOC * b){

  double k=0;
 
         if(a->num_of_vectors==0 || b->num_of_vectors==0) return 0;
 
          k+=basic_kernel(kernel_parm, a, b, 0, 0);
          sqrt(a->vectors[0]->twonorm_STD * b->vectors[0]->twonorm_STD);
          k+=basic_kernel(kernel_parm, a, b, 1, 1)/
          sqrt(a->vectors[1]->twonorm_STD * b->vectors[1]->twonorm_STD);
          k-=basic_kernel(kernel_parm, a, b, 0, 1)/
          sqrt(a->vectors[0]->twonorm_STD * b->vectors[1]->twonorm_STD);
          k-=basic_kernel(kernel_parm, a, b, 1, 0)/
          sqrt(a->vectors[1]->twonorm_STD * b->vectors[0]->twonorm_STD);
   return k;
}


double vector_sequence_ranking(KERNEL_PARM * kernel_parm, DOC * a, DOC * b){

  double k=0;

  k+=sequence_ranking(kernel_parm, a, b, 0, 0); // ranking with sequences of vectors
  k+=sequence_ranking(kernel_parm, a, b, 1, 1);
  k-=sequence_ranking(kernel_parm, a, b, 0, 1);
  k-=sequence_ranking(kernel_parm, a, b, 1, 0);
  
  return k;
}


/* uses all the vectors in the vector set for ranking */
/* this means that there are n/2 vectors for the first pair and n/2 for the second pair */

double sequence_ranking(KERNEL_PARM * kernel_parm, DOC * a, DOC * b, int memberA, int memberB){//all_vs_all vectorial kernel

  int i;
  int startA, startB;
  
  double k=0;
  
   startA= a->num_of_vectors*memberA/2;
   startB= b->num_of_vectors*memberB/2;
   
   if(a->num_of_vectors==0 || b->num_of_vectors==0) return 0;
   
//   for(i=0; i< a->num_of_vectors/2 && i< b->num_of_vectors/2; i++)
  for(i=0; i<1 && i< a->num_of_vectors/2 && i< b->num_of_vectors/2 ; i++)     
      if(a->vectors[i+startA]!=NULL && b->vectors[startB+i]!=NULL){
         k+= basic_kernel(kernel_parm, a, b, startA+i, startB+i)/
         sqrt(a->vectors[startA+i]->twonorm_STD * b->vectors[startB+i]->twonorm_STD);
      }
   return k;
}

 
 
// select the method to combine a vector of tree kernel
// when will be available more kernel type use first_kernel option -F

double choose_tree_kernel(KERNEL_PARM *kernel_parm, DOC *a, DOC *b){
     /* calculate the kernel function */

  switch(kernel_parm->vectorial_approach_tree_kernel) {

    case 'S': /* TREE KERNEL Sequence k11+k22+k33+..+knn*/
             return sequence_tree_kernel(kernel_parm,a,b);; 
    case 'A': /* TREE KERNEL ALL-vs-ALL k11+k12+k13+..+k23+k33+..knn*/
            return(AVA_tree_kernel(kernel_parm,a,b));         	     
    case 'R': /* re-ranking kernel classic SST*/
            return((CFLOAT)tree_kernel_ranking(kernel_parm,a,b));
 //   case 7: /* TREE KERNEL MAX of ALL-vs-ALL */
//            return(AVA_MAX_tree_kernel(kernel_parm,a,b));         	     
//    case 8: /* TREE KERNEL MAX of sequence of pairs Zanzotto et all */
//            return(AVA_MAX_tree_kernel_over_pairs(kernel_parm,a,b));         	     
    default: printf("Error: Unknown tree kernel function\n"); exit(1);
   }
}


// select the method to combine a vector of standard kernel

double choose_second_kernel(KERNEL_PARM *kernel_parm, DOC *a, DOC *b) 
     /* calculate the kernel function */
{
  switch(kernel_parm->vectorial_approach_standard_kernel) {

   case 'S':/* non structured KERNEL Sequence k11+k22+k33+..+knn*/
            return(sequence(kernel_parm,a,b)); 
   case 'A': /* Linear KERNEL ALL-vs-ALL k11+k12+k13+..+k23+k33+..knn*/
            return(AVA(kernel_parm,a,b));
   case 'R': /* re-ranking kernel classic SST*/
            return((CFLOAT)vector_ranking(kernel_parm,a,b));
    case 'I': /* assume instance weight  */
      //printf("using instance weighting!\n");
            return(instance_weight(kernel_parm,a,b)); 
         	     
//    case 13: /* Linear KERNEL MAX of ALL-vs-ALL */
//            return((CFLOAT)AVA_MAX(kernel_parm,a,b));         	     
//    case 14: /* TREE KERNEL MAX of sequence of pairs Zanzotto et all */
//            return((CFLOAT)AVA_MAX_over_pairs(kernel_parm,a,b));         	     
    default: printf("Error: Unknown kernel combination function\n"); exit(1);
   }
}


// select the method to combine a standard kernel with a tree kernel

double advanced_kernels(KERNEL_PARM * kernel_parm, DOC * a, DOC * b){

  //printf("***** advanced_kernels *****\n"); fflush(stdout);

  double k1,
         k2;
/* TEST
        tmp = (k1*k2);
     	printf("K1 %f and K2= %f NORMA= %f norma.a= %f  norma.b= %f\n",k1,k2,norma,a->twonorm_sq,b->twonorm_sq);
	printf("\nKernel Evaluation: %1.20f\n", tmp);
*/

 switch(kernel_parm->combination_type) {

    case '+': /* sum first and second kernels*/
              k1 = choose_tree_kernel(kernel_parm, a, b);
              k2 = choose_second_kernel(kernel_parm, a, b);
    	      //return k2 + kernel_parm->tree_constant*k1; //B: why was this here already weighted?
	      return k2+k1;
    case 'p': k1 = choose_tree_kernel(kernel_parm, a, b); //B: this was '*' before, renamed to 'p' for product
              k2 = choose_second_kernel(kernel_parm, a, b);
              return k1*k2;
    case 'L': /* B: weighted combination: tree_constant = alpha (alpha*ets)+(1-alpha)*ct */
              k1 = choose_tree_kernel(kernel_parm, a, b);
              k2 = choose_second_kernel(kernel_parm, a, b);
	      return kernel_parm->tree_constant * k2 + (1-kernel_parm->tree_constant) * k1;
    case 'T': /* only one kernel*/
              return choose_tree_kernel(kernel_parm, a, b);
    case 'V': /* only one kernel*/
              return choose_second_kernel(kernel_parm, a, b); 
              // otherwise evaluate the vectorial kernel on the basic kernels
 
    default: printf("Error: Unknown kernel combination\n"); fflush(stdout); exit(1);
   }
}
