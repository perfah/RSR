if [[ $1 == 'revert' ]]; then
    rm -r index
    mv index_bak index
    echo 'Reverted!'
else
    cp -r index index_bak
    ./indexer --purge $1
    echo 'Purge complete + backup saved'
fi
