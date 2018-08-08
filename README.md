# NetLogoPlayground
This is just a little experimentation for a NetLogo extension

The idea is to have a history of commands that the agents executed. In order to do that, reporters need to be evaluated
before they are executed by each agent. This is because an agent makes a decision based on the arguments of some commands: 
e.g. fd random 9 - we need to know the correct result returned from "random 9".
More complex examples are the usage of functions such as in-radius or in-cone - where we would like to know which agents were 
possibly reported that cuased some behavior. 

The idea is to then send that information - history of each agent to a special file or WEB API endpoint.
Afterwards NetLogo web could be modified to show the executed history of each agent and allow the user to "replay"
the agents' behavior. This could potentially be useful in an educational or debug scenario. 
