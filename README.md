# TransformX-Service-UCD-CalculatePayments

This project defines the code for generating payments for Closing Disclosure and LOAN Estimate

This service runs on port: 9017

To run the server ,enter into project folder and run

mvn spring-boot:run (or) java -jar *location of the jar file*

The above line will start the server at port 9017

If you want to change the port .Please start th server as mentioned below 

syntax : java -jar *location of the jar file* --server.port= *server port number*
 
example: java -jar target/TRIDentCalculations.jar --server.port=9090

API to generate projectedPayments response(/actualize/transformx/services/ucd/calculatepayments) with input as MISMO XML 
Syntax : *server address with port*/actualize/transformx/services/ucd/calculatepayments; method :POST; 
Header: Content-Type: application/xml

API to check the status of service(/actualize/transformx/services/ucd/calculatepayments/ping)
Syntax : *server address with port*/actualize/transformx/services/ucd/calculatepayments/ping; method: GET;

example: http://localhost:9017/actualize/transformx/services/ucd/calculatepayments ; method: POST; Header: Content-Type:application/xml
