https://qiita.com/drafts/5ea2b225a79d1eea46e6

Note: testはそこそこな課金になるのでご注意

実行時に環境変数指定:
GOOGLE_APPLICATION_CREDENTIALS=/path/to/road-to-iot-8efd3bfb2ccd.json

## test_set1.kt
```
set() 直列 100[query]: 237.52 [ms]
set() 非同期 100[query]: 4.89 [ms]
set() 非同期 500[query]: 7.44 [ms]
```

## test_get1.kt
```
get() 500[doc]: 5.258 [ms/doc]
get() 500[doc]: 1.168 [ms/doc]
get() 500[doc]: 1.142 [ms/doc]
get() 500[doc]: 1.072 [ms/doc]
get() 1000[doc]: 0.629 [ms/doc]
get() sort 1000[doc]: 0.849 [ms/doc]
```

## test_set2.kt
```
set() 100[query] 単一doc seq wait: 134.84 [ms/doc]
set() 100[query] 単一doc para wait: 10.08 [ms/doc]
set() 100[query] 単一doc seq nowait: 0.16 [ms/doc]
```
