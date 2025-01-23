# Dynamic Fraud Detection Demo with Apache Flink

## Introduction


### Instructions (local execution with netcat):

1. Start `netcat`:
```
nc -lk 9999
```
2. Run main method of `com.fraud_detection.Main`
3. Submit to netcat in correct format:
strategy, (strategy_state), (events), (aggregation keys), (aggregateFieldName field), (aggregation function), (limit operator), (limit), (window size in minutes)

##### Examples:

1,(active),(pay),(paymentType),,(paymentAmount),(SUM),(>),(50),(20)  
1,(delete),(pay&refund),(paymentType),,(paymentAmount),(SUM),(>),(50),(20)  
2,(active),(refund),(payeeId),,(paymentAmount),(SUM),(>),(10),(20)  
2,(pause),(pay&refund),(payeeId),,(paymentAmount),(SUM),(>),(10),(20)

##### Examples JSON:  
{ "strategyId": 1, "strategyState": "ACTIVE", "events":["pay", "refund"], groupingKeyNames": ["paymentType"], "aggregateFieldName": "paymentAmount", "aggregatorFunctionType": "SUM","limitOperatorType": "GREATER","limit": 500, "windowMinutes": 20}

##### Examples of Control Commands:

{"strategyState": "CONTROL", "controlType":"DELETE_STRATEGIES_ALL"}  
{"strategyState": "CONTROL", "controlType":"EXPORT_STRATEGIES_CURRENT"}  
{"strategyState": "CONTROL", "controlType":"CLEAR_STATE_ALL"}  


##### Examles of CLI params:
--data-source kafka --strategies-source kafka --alerts-sink kafka --strategies-export-sink kafka

##### Special functions:
1,(active),(eventName),(paymentType),,(COUNT_FLINK),(SUM),(>),(50),(20)
