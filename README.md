# BioLockJ
BioLockJ is a light-weight, extensible, metagenomics pipeline designed to improve the speed, accuracy, and reproducibility of 16s amplicon and whole genome sequencing (WGS) data analysis.  BioLockJ runs on any Linux system (and by extension OSX) but is most powerful in a high performance computing environment by utilizing its parallel processing capabilities.  

Pipeline execution is guided by a single BioLockJ configuration file which can be used to reproduce your analysis and serves to document all runtime parameters.  BioLockJ properties *will appear in italics* throughout this text.

| Primary Inputs | Description | 
| :--- | :---| 
| *input.dirs* | Fast-A/Fast-Q sequence files | 
| *metadata.file* | Path to metadata file |  
| *metadata.descriptor* | Path to descriptor file |  

BioLockJ writes and executes bash shell scripts that call command line bioinformatics tools based on user input, provided via the BioLockJ configuration file.  Generated scripts are reliable, organized, efficient, and reproducible. 

## Execution Summary
1.	Validate metadata and substitute <i>metadata.nullChar</i> for empty cells
2.	Format sequences to meet classifier specifications
3.	Trim primers, merge paired reads, and rarefy seqs if needed
4.	Classify sequences using RDP, QIIME, Kraken, MetaPhlAn, or SLIMM
5.	Generate raw count and relative abundance tables
6.	Build statistical models to find significant OTUs correlated with metadata
7.	Generate summary tables and PDF reports to assess p-value and R2 values
8.	Email user a summary report upon completion

https://github.com/msioda/BioLockJ/blob/master/doc/img/BioLockJ_Flowchart.png

## System Diagram

![alt text](https://github.com/msioda/BioLockJ/blob/master/doc/img/BioLockJ_Flowchart.png "BioLockJ System Diagram")

