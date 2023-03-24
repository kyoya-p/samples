import { decode, Jwt, sign, verify } from 'jsonwebtoken'

const originalToken = process.argv[2]
const secretSign = process.argv[3]
const expireTime = process.argv[4] // 2023-03-23T23:59:59+09:00

if (originalToken === undefined) {
    console.log("Syntax: npx ts-node exp.ts <original-token> [<signature> [<new-date>]]")
} else if (secretSign === undefined) {
    const decodedToken = decode(originalToken, { json: true, complete: true })!
    console.log(decodedToken.payload)
} else if (expireTime === undefined) {
    try {
        verify(originalToken, secretSign, { complete: true })
        console.log(`correct`)
        process.exit(0)
    } catch (ex: unknown) {
        console.log(`incorrect`)
        process.exit(-1)
    }
} else {
    const decodedToken = verify(originalToken, secretSign, { complete: true })
    const newTime = new Date(expireTime)
    let newPayload = decodedToken.payload as { exp: number }
    newPayload.exp = Math.floor(newTime.getTime() / 1000)
    const jwtToken = sign(newPayload, secretSign!, { algorithm: 'HS256' })
    console.log(jwtToken)
}
