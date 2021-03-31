# iti0215-2021-ledger

### ledger-api

Kloonide käivitamiseks pane nende IPd `data/ip.txt` faili

### API käsklused kuidas erinevad kloonid omavahel suhelda saavad

`GET/POST http://hostip:port/addr`

Kasutatakse IP aadresside jagamiseks. GET käsklus jooksutatakse kohe alguses 
ning selle info alusel saadakse ühelt teadaolevalt hostilt kõik talle teadaolevad kloonid.
POST käsklusega saadab kloon kõikidele kloonidele info et on olemas.


`GET http://hostip:port/getblocks |
http://hostip:port/getblocks/(hash)`

Selle alusel saab pärida kas kõiki blokke või ainult neid teatud hashist.

`GET http://hostip:port/getdata/(hash)`

Selle abil saab kindla hashiga blocki sisu kätte

`POST http://hostip:port/transaction`
```
{
"message": "content_string",
"transaction": 5555
}
```

Lisab uue tehingu ledgerisse ning käskluse `POST http://hostip:port/block` edasi teistele ledgeritele.



#### JAR faili käivitamine
- Käivitada käsklus
```
java -jar ledger-api\build\libs\ledger-api-all.jar
```
