import axios, {} from "axios";
import https from 'https' // 追加

const url = "https://10.36.102.120";

const instance = axios.create({
    httpsAgent: new https.Agent({  
      rejectUnauthorized: false
    })
  });

async function main(){
    const directory = await instance.get(url + ":14000/dir");
    console.log(directory.data)
}

main()
