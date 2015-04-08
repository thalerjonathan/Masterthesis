function [ data ] = matchingProbAssetLoan( N )
%MATCHINGPROB Summary of this function goes here
%   Detailed explanation goes here

    % generate N agents in range of [0..1]
    % asume ASCENDING_CONNECTIVITY
    % calculate pairwise probability varying over q (loan price)
    agents = zeros( N, 1 );
    data = zeros( N, 1 );
    
    for i = 0 : N
        agents( i + 1 ) = i / N;
    end
    
    q = 0.2;
    
    expectedJ = 0.2;
    pU = 1.0;
    pD = 0.2;
 
    for i = 0 : N - 1
        ha = agents( i + 1 );
        hb = agents( i + 2 );
        
        cHa = ( ha * pU + ( 1.0 - ha ) * pD ) / expectedJ;
        cHb = ( hb * pU + ( 1.0 - hb ) * pD ) / expectedJ;
        
        p = ( ( cHb - cHa )^2 ) / ( ( cHb - ( pD / q ) ) * ( ( pU / q ) - cHa ) );
        
        data( i + 1 ) = p;
    end
end

