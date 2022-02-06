

https://developers.google.com/closure/compiler/docs/gettingstarted_ui
https://closure-compiler.appspot.com/

https://qiita.com/kanaxx/items/63debe502aacd73c3cb8

```javascript
javascript:(
()->
alert('はろーわーるど');
)()
```

//javascript:(function(){s=prompt('文字列');})()

//javascript:(()=>{s=prompt('SSS');

})()

```
javascript:(function(){
    walkTextNode(document);
    function walkTextNode(items){
        var cs=items.childNodes;
        for(e in cs) {
        	if(typeof e == "string"){
            alert(e.innerText);
        	}
        }
    }
}
)()
```
