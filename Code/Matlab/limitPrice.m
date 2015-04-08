function [ output_args ] = limitPrice( N )
%LIMITPRICE Summary of this function goes here
%   Detailed explanation goes here

    % generate N agents in range of [0..1]
    % asume ASCENDING_CONNECTIVITY
    % calculate pairwise probability varying over q (loan price)
    agents = zeros( N, 1 );
    data = zeros( N, 1 );
    xLabels = [];
    
    for i = 0 : N
        agents( i + 1 ) = i / N;
        xLabels{ i + 1 } = { sprintf( '%d (%0.2f)', i, agents( i + 1 ) ) };
    end

    % value of tomorrows UP
    pU = 1.0;
    % value of tomorrows DOWN
    pD = 0.2;
    
    for i = 0 : N
        h = agents( i + 1 );

        limitPriceAsset = h * pU + ( 1 - h ) * pD;

        data( i + 1 ) = limitPriceAsset;
    end
    
    plot( data, '-X' );
    title( sprintf( 'Limit Price for Asset of %d Agents', ( N + 1 ) ) );
    legend( 'Price' );
    xlabel( 'Agent' );
    ylabel( 'Limit-Price' );
    set(gca, 'XTick', 1:length(xLabels)); % Change x-axis ticks
    ticLoc = get( gca, 'XTick' );
    ticLab = cellfun(@(x) char( xLabels{ x } ),num2cell(ticLoc),'UniformOutput',false);
    set(gca,'XTickLabel', ceil(agents*1000)/1000); % Change x-axis ticks labels to desired values.
end

