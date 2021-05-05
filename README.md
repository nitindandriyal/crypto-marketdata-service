# Crypto Mock Market Data Service

This service generates mock market data based on brownian motion.

There are some seed prices and some other parameters 

Service is single threaded and generates market data at some random interval between 500ms to 2000ms.
After generating the prices, it publishes the market data as sequence of bytes 
- TICKER(always 4 chars)
- generated price as double
  
Above is kept intentionally and for the sake of simplicity in the protocol, and its faster :) 

For a complex messaging protocol SBE or protobuf can be used
