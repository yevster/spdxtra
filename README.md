![SpdXtra](http://s1.postimg.org/y28xxq473/image_2.jpg)

[![Build Status](https://travis-ci.org/yevster/spdxtra.svg?branch=master)](https://travis-ci.org/yevster/spdxtra)

##Project Description

SPDX is a standard, championed by The Linux Foundation, for describing the contents and, most importantly, licensing information for software products.

SpdXtra is a Java API for building and analyzing SPDX for large codebases. Unlike SPDXtools, the document model is not built or kept in memory.

At present, the API is sufficient to create minimal SPDX 2.1 documents, but is far from feature complete. For example(s) of what is currently possible with the API, a demo/tutorial is available [here](https://bitbucket.org/yevster/spdxtraxample).

In addition, the command line tool can be used to convert SPDX from RDF form to JSON-LD - if that's your desire. Once the minimal document, package, and file properties mandated by the SPDX 2.0 specification are implemented, the first release will be made.

##So how does it work?

SpdXtra is designed to perform tractibly when generating or analyzing SPDX for very large codebases (hundreds of thousands of files).

At its core, SpdXtra is a library static (as in "completely stateless") SPDX-specific operations to populate and access a TDB dataset.. It has two top-level classes with static methods:

`Read` - Return immutable views over specific SPDX elements and to output the datastore to a file.

`Write` - To generate SPDX-specific inserts and updates to the dataset and apply them in a transactional way. Grouping updates into transactions can ensure atomicity of multiple updates to the same element and improved performance (multiple updates on the same resource can be persisted at once).

##Limitations (important!)

The TDB datastore used by SpdXtra may not be used concurrently by multiple processes.

##License

This product is licensed under the Apache 2.0 License.

All documents produced by this product are licensed under the Creative Commons Zero v1.0 Universal license, per Section 2 of the SPDX Specification.
