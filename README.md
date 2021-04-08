# iti0215-2021-ledger

### ledger-api

### API käsklused, mida kloonid kasutavad omavahel suhtlemiseks

* `GET http://hostip:port/addr`

  Näitab ühe klooni või masteri juures olevaid IP aadresse.
  Kasutatakse IP-aadresside jagamiseks. GET käsklus jooksutatakse kohe alguses 
  ning selle info alusel saadakse ühelt teadaolevalt hostilt kõik talle teadaolevad kloonid.

* `POST http://hostip:port/addr`
  
  Saadab IP aadressi ühele kloonile edasi.
  POST käsklusega saadab kloon kõikidele kloonidele start up ajal info oma IP aadressite kohta.

* `GET http://hostip:port/getblocks | http://hostip:port/getblocks/(hash)`

  Selle alusel saab pärida kas kõiki blokke või ainult neid teatud hashist.

* `GET http://hostip:port/getdata/(hash)`

  Selle abil saab kindla hashiga blocki sisu kätte.

* `POST http://hostip:port/transaction`
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
  java -jar ledger-api\build\libs\ledger-api-all.jar ip:port
  ```
