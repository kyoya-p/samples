import {
  ServerStreamingCall,
    ClientCallDetails,
      Status,
      } from "@grpc/grpc-js";

interface MyRequest {
  name: string;
  }

interface MyResponse {
  message: string;
  }

class MyService {
  async hello(call: ServerStreamingCall<MyRequest, MyResponse>) {
      for (let i = 0; i < 10; i++) {
            call.write(new MyResponse({ message: `Hello, ${call.details.remoteAddress}!` }));
	        }
		    call.end();
		      }
		      }

const server = new grpc.Server({
  port: 50051,
  });
  server.addService(new MyService());
  server.start();

const client = new grpc.Client({
  target: "localhost:50051",
  });
  const service = client.getService("MyService");

const callback = (response: MyResponse, status: Status) => {
  console.log(response.message);
  };

service.hello({ name: "John Doe" }, callback);

