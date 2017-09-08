#!/usr/bin/env bash

# Non-optimized single protein query using current code - takes about 1 hour
# run from within 3DTS folder
uniprotofinterest="Q9Y478" # edit uniprot ID here - will work if pdbs are already linked in uniprot text file

### Do not modify below
wget -O - "http://www.uniprot.org/uniprot/$uniprotofinterest.txt" | gzip -c > input/uniprot.gz # get single protein uniprot
zgrep $uniprotofinterest input/gencode.v26lift37.metadata.SwissProt.gz > input/gencode.v26lift37.metadata.SwissProt_query.gz # capture relevant data for joins
mv input/gencode.v26lift37.metadata.SwissProt_query.gz input/gencode.v26lift37.metadata.SwissProt.gz
zgrep $uniprotofinterest input/gencode.v26lift37.metadata.SwissProt.gz | cut -f 1 | zgrep -f - input/gencode.v26lift37.annotation.gtf.gz | gzip -c > input/gencode.v26lift37.annotation.gtf_query.gz # capture relevant data for joins
mv input/gencode.v26lift37.annotation.gtf_query.gz input/gencode.v26lift37.annotation.gtf.gz
### Do not modify above


# Runs 3DTS
target/universal/stage/bin/saturation -Dconfig.file=input/conf -J-Xmx115G -Djava.io.tmpdir=tmp/ -Dfile.encoding=UTF-8
