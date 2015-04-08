function [ individualP, matchingP ] = matchingProbAssetCash_FC( N )
%MATCHINGPROBASSETCASH Summary of this function goes here
%   Detailed explanation goes here

    agents = zeros( N, 1 );
    individualP = zeros( N, 2 );
    matchingP = zeros( N, 1 );
    xLabels = [];
    
    for i = 0 : N
        agents( i + 1 ) = i / N;
        
        a = { sprintf( '%d (%0.2f)', i, agents( i + 1 ) ) };
        xLabels{ i + 1 } = a;
    end
    
    pU = 1.0;
    pD = 0.2;
  
    for i = 0 : N - 1
        indexA = i + 1;
        ha = agents( indexA );
        limitPriceAsk = ha * pU + ( 1.0 - ha ) * pD;
  
        for j = 0 : N - 1
            indexB = j + 1;
            hb = agents( indexB );

            limitPriceBid = hb * pU + ( 1.0 - hb ) * pD;
            
            limitPriceRange = limitPriceBid - limitPriceAsk;

            % probability for the bider to fall into the potential matching
            % range
            pBid = limitPriceRange / ( limitPriceBid - pD );
             % probability for asker to fall into the potential matching range
            pAsk = limitPriceRange / ( pU - limitPriceAsk );
            
            % calculate probabilities that asker AND bider fall both into
            % matching range: bayes theorem, just multiply
            matchingP( j + 1 ) = pAsk * pBid;
        end
        
        figure
    
        plot( matchingP, '-X' );
        title( sprintf( '%0.3f matching with other agents', ha ) );
        legend( 'Matching-Probability' );
        xlabel( 'Agent' );
        ylabel( 'Probability' );
        set(gca, 'XTick', 1:length(agents)); % Change x-axis ticks
        set( gca,'XTickLabel', ceil(agents*1000)/1000); % Change x-axis ticks labels to desired values.
    end
end

