class Style:
    '''
    entry orders style
    aggressive-> market orders
    passive -> mid price orders
    level ->set by level -1 is market order , 0 is midprice , 1 is best
    '''
    aggressive='aggressive'#market orders
    passive = 'passive'  # mid price orders
    level = 'level'  # pending on best price
