Changes
=======
## 4.0.5 (Jun 2021)
* Updating to Commons 2.2.5

## 4.0.4 (Feb 2021)
* Fixed filepointer path  issue

## 4.0.3 (Jan 2021)
* Updating to Commons 2.2.4

## 4.0.2 (June 2020)
* Fixed metric printing when printMatchedString is enabled

## 4.0.1 (April 2020)
* Upgraded to Extensions SDK v2.2.2 to resolve log4j & slf4j discrepancies
* Extension compatible with MA 4.5.x and 20.3.x+. 

## 4.0.0 (December 2019)
* Added functionality to send log matches to the events server
* Added an offset mechanism to cover data directly following the log match to be included in the event body
* Fixed a bug around redundant metrics 
* Added scripts and integration tests for BTD
* General improvements and tweaks

## 3.0.0 (August 2018)
* Complete revamp using Commons 2.1.0. Support for character sets that are not UTF-8 encoded. General bug fixes, performance improvements and detailed documentation

## 2.0.8 (August 2017)
* Deeper concurrency to cover very fast log rollovers

## 2.0.5 (July 2017)
* Added global occurrence metric for all configured matches 

## 2.0.0 (May 2015) 
* Added concurrency + support for dynamic filename and regex search strings

## 1.1.0 (November 2014) 
* Addition of a flag for reporting a complete matched string

## 1.0.0 (June 2014)
* Initial release
